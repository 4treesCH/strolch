/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.persistence.postgresql;

import static li.strolch.persistence.postgresql.PostgreSqlHelper.toSql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import li.strolch.model.query.AndSelection;
import li.strolch.model.query.IdSelection;
import li.strolch.model.query.NameSelection;
import li.strolch.model.query.NotSelection;
import li.strolch.model.query.OrSelection;
import li.strolch.model.query.ParameterSelection.BooleanParameterSelection;
import li.strolch.model.query.ParameterSelection.DateParameterSelection;
import li.strolch.model.query.ParameterSelection.DateRangeParameterSelection;
import li.strolch.model.query.ParameterSelection.FloatParameterSelection;
import li.strolch.model.query.ParameterSelection.IntegerParameterSelection;
import li.strolch.model.query.ParameterSelection.LongParameterSelection;
import li.strolch.model.query.ParameterSelection.StringListParameterSelection;
import li.strolch.model.query.ParameterSelection.StringParameterSelection;
import li.strolch.model.query.ParameterSelectionVisitor;
import li.strolch.model.query.Selection;
import li.strolch.model.query.StrolchElementSelectionVisitor;
import li.strolch.model.query.StrolchTypeNavigation;
import ch.eitchnet.utils.StringMatchMode;
import ch.eitchnet.utils.dbc.DBC;
import ch.eitchnet.utils.iso8601.ISO8601FormatFactory;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public abstract class PostgreSqlQueryVisitor implements StrolchElementSelectionVisitor, ParameterSelectionVisitor {

	protected StringBuilder sql;
	protected StringBuilder sb;
	protected String type;
	protected List<Object> values;
	protected boolean any;
	protected String indent;
	private String sqlAsString;

	public PostgreSqlQueryVisitor(String fields) {
		this.indent = "";
		this.sql = new StringBuilder();
		this.sb = new StringBuilder();
		this.values = new ArrayList<>();

		this.sql.append("select ");
		this.sql.append(fields);
		this.sql.append("\nfrom\n");
		this.sql.append("  ");
		this.sql.append(getTableName());
		this.indent = "  ";
	}

	public String getSql() {
		if (sqlAsString != null)
			return sqlAsString;

		this.sql.append("\nwhere\n");
		this.sql.append(this.indent);

		if (this.any) {
			this.sql.append("type = ?");
			sqlAsString = this.sql.toString();
			return sqlAsString;
		}

		this.sql.append("type = ? and\n");

		this.sql.append(this.sb.toString());
		sqlAsString = this.sql.toString();
		return sqlAsString;
	}

	/**
	 * @return the any
	 */
	public boolean isAny() {
		return this.any;
	}

	public String getType() {
		return this.type;
	}

	public void validate() {
		DBC.INTERIM.assertNotEmpty("No navigation was set!", this.type);
	}

	protected abstract String getClassName();

	protected abstract String getTableName();

	@Override
	public void visit(StrolchTypeNavigation navigation) {
		this.type = navigation.getType();
	}

	@Override
	public void visit(IdSelection selection) {
		this.sb.append(this.indent);
		List<String> ids = selection.getIds();
		if (ids.isEmpty())
			return;
		int size = ids.size();
		if (size == 1) {
			this.sb.append("id = ?\n");
			this.values.add(ids.get(0));
		} else {
			this.sb.append("id in (");
			Iterator<String> iter = ids.iterator();
			while (iter.hasNext()) {
				String id = iter.next();
				this.sb.append("?");
				this.values.add(id);
				if (iter.hasNext())
					this.sb.append(", ");
			}
			this.sb.append(" )\n");
		}
	}

	@Override
	public void visit(NameSelection selection) {
		this.sb.append(this.indent);
		String name = selection.getName();
		StringMatchMode mm = selection.getMatchMode();
		this.sb.append(toSql("name", this.indent, mm, this.values, name));
	}

	@Override
	public void visitAny() {
		this.any = true;
	}

	@Override
	public void visitAnd(AndSelection andSelection) {
		this.sb.append(this.indent);
		List<Selection> selections = andSelection.getSelections();
		this.sb.append("( \n");
		Iterator<Selection> iter = selections.iterator();
		String indent = this.indent;
		this.indent += "  ";
		while (iter.hasNext()) {
			Selection selection = iter.next();
			selection.accept(this);
			if (iter.hasNext()) {
				this.sb.append(indent);
				this.sb.append("and\n");
			}
		}
		this.indent = indent;
		this.sb.append(this.indent);
		this.sb.append(")\n");
	}

	@Override
	public void visitOr(OrSelection orSelection) {
		this.sb.append(this.indent);
		List<Selection> selections = orSelection.getSelections();
		this.sb.append("( \n");
		Iterator<Selection> iter = selections.iterator();
		String indent = this.indent;
		this.indent += "  ";
		while (iter.hasNext()) {
			Selection selection = iter.next();
			selection.accept(this);
			if (iter.hasNext()) {
				this.sb.append(indent);
				this.sb.append("or\n");
			}
		}
		this.indent = indent;
		this.sb.append(this.indent);
		this.sb.append(")\n");
	}

	@Override
	public void visitNot(NotSelection notSelection) {
		this.sb.append(this.indent);
		List<Selection> selections = notSelection.getSelections();
		this.sb.append("not ( \n");
		Iterator<Selection> iter = selections.iterator();
		String indent = this.indent;
		this.indent += "  ";
		while (iter.hasNext()) {
			Selection selection = iter.next();
			selection.accept(this);
			if (iter.hasNext()) {
				this.sb.append(indent);
				this.sb.append("and\n");
			}
		}
		this.indent = indent;
		this.sb.append(this.indent);
		this.sb.append(")\n");
	}

	private void xpath(String bagKey, String paramKey, String paramValue) {
		String xpath = "cast(xpath('//Resource/ParameterBag[@Id=\"${bagKey}\"]/Parameter[@Id=\"${paramKey}\" and @Value=\"${paramValue}\"]', asxml) as text[]) != '{}'\n";
		this.sb.append(this.indent);
		xpath = xpath.replace("${bagKey}", bagKey);
		xpath = xpath.replace("${paramKey}", paramKey);
		xpath = xpath.replace("${paramValue}", paramValue);
		this.sb.append(xpath);
	}

	@Override
	public void visit(StringParameterSelection selection) {
		String value = selection.getValue();

		String xpath = "xpath('//Resource/ParameterBag[@Id=\"${bagKey}\"]/Parameter[@Id=\"${paramKey}\"]/@Value', asxml))::TEXT AS content";
		xpath = xpath.replace("${bagKey}", selection.getBagKey());
		xpath = xpath.replace("${paramKey}", selection.getParamKey());

		sb.append(this.indent);
		sb.append("id in (\n");
		sb.append(this.indent);
		sb.append("  SELECT id\n");
		sb.append(this.indent);
		sb.append("  FROM (\n");
		sb.append(this.indent);
		sb.append("    SELECT id, UNNEST(");
		sb.append(xpath);
		sb.append("\n");
		sb.append(this.indent);
		sb.append("from ");
		sb.append(getTableName());
		sb.append("\n");
		sb.append(this.indent);
		sb.append(") AS alias\n");
		sb.append(this.indent);
		sb.append("WHERE ");

		if (selection.getMatchMode().isEquals()) {
			if (selection.getMatchMode().isCaseSensitve()) {
				sb.append("content = ?\n");
			} else {
				sb.append("content ILIKE ?\n");
			}
		} else {
			value = "%" + value + "%";
			if (selection.getMatchMode().isCaseSensitve()) {
				sb.append("content LIKE ?\n");
			} else {
				sb.append("content ILIKE ?\n");
			}
		}

		sb.append(this.indent);
		sb.append(")\n");

		this.values.add(value);
	}

	@Override
	public void visit(IntegerParameterSelection selection) {
		xpath(selection.getBagKey(), selection.getParamKey(), selection.getValue().toString());
	}

	@Override
	public void visit(BooleanParameterSelection selection) {
		xpath(selection.getBagKey(), selection.getParamKey(), selection.getValue().toString());
	}

	@Override
	public void visit(LongParameterSelection selection) {
		xpath(selection.getBagKey(), selection.getParamKey(), selection.getValue().toString());
	}

	@Override
	public void visit(FloatParameterSelection selection) {
		xpath(selection.getBagKey(), selection.getParamKey(), selection.getValue().toString());
	}

	@Override
	public void visit(DateParameterSelection selection) {
		xpath(selection.getBagKey(), selection.getParamKey(),
				ISO8601FormatFactory.getInstance().formatDate(selection.getValue()));
	}

	@Override
	public void visit(DateRangeParameterSelection selection) {
		throw new UnsupportedOperationException("Not yet supported!");
	}

	@Override
	public void visit(StringListParameterSelection selection) {
		throw new UnsupportedOperationException("Not yet supported!");
	}

	/**
	 * @param ps
	 * @throws SQLException
	 */
	public void setValues(PreparedStatement ps) throws SQLException {
		if (this.any) {
			ps.setString(1, this.type);
			return;
		}

		ps.setString(1, this.type);
		for (int i = 0; i < this.values.size(); i++) {
			ps.setObject(i + 2, this.values.get(i));
		}
	}
}

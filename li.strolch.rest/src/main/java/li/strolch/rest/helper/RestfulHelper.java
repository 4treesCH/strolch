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
package li.strolch.rest.helper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import li.strolch.model.StrolchRootElement;
import li.strolch.model.visitor.StrolchElementVisitor;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.StrolchRestfulConstants;
import li.strolch.rest.model.QueryData;
import li.strolch.search.RootElementSearchResult;
import li.strolch.utils.collections.Paging;
import li.strolch.utils.dbc.DBC;
import li.strolch.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class RestfulHelper {

	public static Locale getLocale(HttpHeaders headers) {
		if (headers == null || StringHelper.isEmpty(headers.getHeaderString(HttpHeaders.ACCEPT_LANGUAGE)))
			return null;
		return headers.getAcceptableLanguages().get(0);
	}

	public static Certificate getCert(HttpServletRequest request) {
		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);
		DBC.PRE.assertNotNull("Certificate not found as request attribute!", cert);
		return cert;
	}

	public static <T extends StrolchRootElement> JsonObject toJson(QueryData queryData, long dataSetSize,
			RootElementSearchResult<T> result, StrolchElementVisitor<JsonElement> toJsonVisitor) {

		// paging
		Paging<T> paging = result.toPaging(queryData.getOffset(), queryData.getLimit());

		// get page
		List<T> page = paging.getPage();

		JsonObject root = new JsonObject();
		root.addProperty("msg", "-");
		root.addProperty("limit", paging.getLimit());
		root.addProperty("offset", paging.getOffset());
		root.addProperty("size", paging.getSize());
		root.addProperty("previousOffset", paging.getPreviousOffset());
		root.addProperty("nextOffset", paging.getNextOffset());
		root.addProperty("lastOffset", paging.getLastOffset());

		root.addProperty("dataSetSize", dataSetSize);

		if (StringHelper.isNotEmpty(queryData.getOrderBy()))
			root.addProperty("sortBy", queryData.getOrderBy());
		root.addProperty("descending", queryData.isDescending());

		// add items
		JsonArray data = new JsonArray();
		for (T t : page) {
			JsonElement element = t.accept(toJsonVisitor);
			data.add(element);
		}
		root.add("data", data);
		return root;
	}
}

/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of the XXX.
 *
 *  XXX is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  XXX is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XXX.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package li.strolch.runtime.query.inmemory;

import java.util.Date;
import java.util.List;

import li.strolch.model.GroupedParameterizedElement;
import li.strolch.model.ParameterBag;
import li.strolch.model.parameter.BooleanParameter;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.FloatParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.parameter.LongParameter;
import li.strolch.model.parameter.StringListParameter;
import li.strolch.model.parameter.StringParameter;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public abstract class ParameterSelector<T extends GroupedParameterizedElement> implements Selector<T> {

	protected String bagKey;
	protected String paramKey;

	public ParameterSelector(String bagKey, String key) {
		this.bagKey = bagKey;
		this.paramKey = key;
	}

	@Override
	public abstract boolean select(GroupedParameterizedElement element);

	public static <T extends GroupedParameterizedElement> Selector<T> stringSelector(String bagKey, String paramKey,
			String value) {
		return new StringParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> integerSelector(String bagKey, String paramKey,
			int value) {
		return new IntegerParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> booleanSelector(String bagKey, String paramKey,
			boolean value) {
		return new BooleanParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> floatSelector(String bagKey, String paramKey,
			double value) {
		return new FloatParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> longSelector(String bagKey, String paramKey,
			long value) {
		return new LongParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> dateSelector(String bagKey, String paramKey,
			Date value) {
		return new DateParameterSelector<>(bagKey, paramKey, value);
	}

	public static <T extends GroupedParameterizedElement> Selector<T> stringListSelector(String bagKey,
			String paramKey, List<String> value) {
		return new StringListParameterSelector<>(bagKey, paramKey, value);
	}

	private static class StringParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private String value;

		public StringParameterSelector(String bagKey, String paramKey, String value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {

			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			StringParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class IntegerParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private Integer value;

		public IntegerParameterSelector(String bagKey, String paramKey, Integer value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			IntegerParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class BooleanParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private Boolean value;

		public BooleanParameterSelector(String bagKey, String paramKey, Boolean value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			BooleanParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class FloatParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private Double value;

		public FloatParameterSelector(String bagKey, String paramKey, Double value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			FloatParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class LongParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private Long value;

		public LongParameterSelector(String bagKey, String paramKey, Long value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			LongParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class DateParameterSelector<T extends GroupedParameterizedElement> extends ParameterSelector<T> {

		private Date value;

		public DateParameterSelector(String bagKey, String paramKey, Date value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			DateParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}

	private static class StringListParameterSelector<T extends GroupedParameterizedElement> extends
			ParameterSelector<T> {

		private List<String> value;

		public StringListParameterSelector(String bagKey, String paramKey, List<String> value) {
			super(bagKey, paramKey);
			this.value = value;
		}

		@Override
		public boolean select(GroupedParameterizedElement element) {
			if (!element.hasParameterBag(this.bagKey))
				return false;

			ParameterBag bag = element.getParameterBag(this.bagKey);
			if (!bag.hasParameter(this.paramKey))
				return false;

			StringListParameter param = bag.getParameter(this.paramKey);
			return param.getValue().equals(this.value);
		}
	}
}

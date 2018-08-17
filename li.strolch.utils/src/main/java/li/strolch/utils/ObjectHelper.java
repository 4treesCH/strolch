package li.strolch.utils;

import java.util.Collection;
import java.util.Iterator;

public class ObjectHelper {

	public static boolean equals(Object left, Object right, boolean ignoreCase) {
		if (left == null && right == null)
			return true;
		if (left == right)
			return true;
		if (left == null || right == null)
			return false;

		if (left instanceof Collection) {
			Collection<?> leftCollection = (Collection) left;

			if (right instanceof Collection) {
				Collection<?> rightCollection = (Collection) right;
				if (leftCollection.size() != rightCollection.size())
					return false;

				Iterator<?> leftIter = leftCollection.iterator();
				Iterator<?> rightIter = rightCollection.iterator();

				while (leftIter.hasNext()) {
					Object l = leftIter.next();
					Object r = rightIter.next();

					// since we ignore case, we can toString()
					if (ignoreCase) {
						if (!l.toString().equalsIgnoreCase(r.toString()))
							return false;
					} else {
						if (!l.equals(r))
							return false;
					}
				}

				return true;
			}

			if (right instanceof String[]) {
				String[] rightArr = (String[]) right;

				int i = 0;
				Iterator<?> leftIter = leftCollection.iterator();
				while (leftIter.hasNext()) {
					Object l = leftIter.next();
					Object r = rightArr[i];

					// since we ignore case, we can toString()
					if (ignoreCase) {
						if (!l.toString().equalsIgnoreCase(r.toString()))
							return false;
					} else {
						if (!l.equals(r))
							return false;
					}

					i++;
				}

				return true;
			}

			// since right is neither a collection nor an array, we can't check for equals!
			return false;
		}

		if (left.getClass() != right.getClass())
			return false;

		// since we ignore case, we can toString()
		if (ignoreCase)
			return left.toString().equalsIgnoreCase(right.toString());

		return left.equals(right);
	}

	public static int compare(Object left, Object right, boolean ignoreCase) {
		if (left == right)
			return 0;
		if (left == null)
			return -1;
		if (right == null)
			return 1;

		if (ignoreCase && left instanceof String && right instanceof String)
			return ((String) left).compareToIgnoreCase((String) right);

		if (left instanceof Comparable) {
			@SuppressWarnings("unchecked")
			Comparable<Object> comparable = (Comparable<Object>) left;

			return comparable.compareTo(right);
		}

		int answer = left.getClass().getName().compareTo(right.getClass().getName());
		return (answer == 0) ? left.hashCode() - right.hashCode() : answer;
	}

	public static boolean contains(Object left, Object right, boolean ignoreCase) {
		if (left == null && right == null)
			return true;
		if (left == null)
			return false;
		if (right == null)
			return false;

		if (left instanceof Collection) {
			Collection<?> leftCollection = (Collection) left;

			if (right instanceof Collection) {
				Collection<?> rightCollection = (Collection) right;
				for (Object l : leftCollection) {
					for (Object r : rightCollection) {
						if (contains(l, r, ignoreCase))
							return true;
					}
				}

				return false;
			}

			if (right instanceof String[]) {
				String[] rightArr = (String[]) right;
				for (Object l : leftCollection) {
					for (Object r : rightArr) {
						if (contains(l, r, ignoreCase))
							return true;
					}
				}

				return false;
			}

			for (Object l : leftCollection) {
				if (contains(l, right, ignoreCase))
					return true;
			}

			return false;
		}

		if (left instanceof String) {
			String leftString = (String) left;

			if (right instanceof String[]) {
				String[] rightArr = (String[]) right;

				if (ignoreCase) {
					leftString = leftString.toLowerCase();
					for (String s : rightArr) {
						if (!leftString.contains(s.toLowerCase()))
							return false;
					}

					return true;

				} else {
					for (String s : rightArr) {
						if (!leftString.contains(s))
							return false;
					}

					return true;
				}
			}

			if (right instanceof String) {
				String rightString = (String) right;

				if (ignoreCase)
					return leftString.toLowerCase().contains(rightString.toLowerCase());
				else
					return leftString.contains(rightString);
			}
		}

		// comparing non-strings we use equals, as contains fits as well
		if (left.getClass() == right.getClass())
			return left.equals(right);

		throw new IllegalArgumentException("Unhandled type combination " + left.getClass() + " / " + right.getClass());
	}

	public static boolean isIn(Object left, Object right, boolean ignoreCase) {
		if (left == null && right == null)
			return true;
		if (left == null)
			return false;
		if (right == null)
			return false;

		if (right instanceof Collection) {
			if (left instanceof Collection) {
				Collection<?> collectionRight = (Collection) right;
				Collection<?> collectionleft = (Collection) left;
				for (Object oLeft : collectionleft) {
					for (Object oRight : collectionRight) {
						if (equals(oLeft, oRight, ignoreCase))
							return true;
					}
				}
				return false;
			} else {
				Collection<?> collection = (Collection) right;
				for (Object o : collection) {
					if (equals(left, o, ignoreCase))
						return true;
				}

				return false;
			}

		}

		if (right instanceof Object[]) {
			Object[] arr = (Object[]) right;
			for (Object o : arr) {
				if (equals(left, o, ignoreCase))
					return true;
			}

			return false;
		}

		if (right instanceof String || right instanceof Number) {
			return equals(left, right, ignoreCase);
		}

		throw new IllegalArgumentException("Unhandled type combination " + left.getClass() + " / " + right.getClass());
	}

	public static boolean startsWith(Object left, Object right, boolean ignoreCase) {
		if (left == null && right == null)
			return true;
		if (left == null)
			return false;
		if (right == null)
			return false;

		if (left instanceof String && right instanceof String) {
			String str = (String) left;
			String subStr = (String) right;

			if (ignoreCase)
				return str.toLowerCase().startsWith(subStr.toLowerCase());
			return str.startsWith(subStr);
		}

		throw new IllegalArgumentException("Unhandled type combination " + left.getClass() + " / " + right.getClass());
	}

	public static boolean endsWith(Object left, Object right, boolean ignoreCase) {
		if (left == null && right == null)
			return true;
		if (left == null)
			return false;
		if (right == null)
			return false;

		if (left instanceof String && right instanceof String) {
			String str = (String) left;
			String subStr = (String) right;

			if (ignoreCase)
				return str.toLowerCase().endsWith(subStr.toLowerCase());
			return str.endsWith(subStr);
		}

		throw new IllegalArgumentException("Unhandled type combination " + left.getClass() + " / " + right.getClass());
	}
}

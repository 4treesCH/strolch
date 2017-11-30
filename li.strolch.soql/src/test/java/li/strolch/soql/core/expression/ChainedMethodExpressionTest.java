package li.strolch.soql.core.expression;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import li.strolch.soql.core.MockObject;
import li.strolch.soql.core.MockParameter;
import li.strolch.soql.core.expresssion.ChainedMethodExpression;
import li.strolch.soql.core.expresssion.MethodArgumentDeclaration;
import li.strolch.soql.core.expresssion.MethodExpression;
import li.strolch.soql.core.expresssion.ParameterReference;

public class ChainedMethodExpressionTest {

	@Test
	public void test() {

		String matchingKey = "testString";

		// build the message expression
		ParameterReference parameterReference = new ParameterReference();
		parameterReference.varName = "param_1";

		MethodArgumentDeclaration argument = new MethodArgumentDeclaration();
		argument.addParameterReference(parameterReference);

		MethodExpression methodExpression = new MethodExpression();
		methodExpression.setMethodName("getParameter");
		methodExpression.setMethodArguments(argument);

		// build the chained expression
		ChainedMethodExpression chainedMethodExpression = new ChainedMethodExpression();
		chainedMethodExpression.setObjectKey("a");
		chainedMethodExpression.addMethodExpression(methodExpression);

		// prepare the runtime objects
		MockObject mockObject = new MockObject();
		mockObject.putParameter(matchingKey, new MockParameter());

		Map<String, Object> inputObjects = new HashMap<>();
		inputObjects.put("a", mockObject);

		Map<String, Object> queryParameter = new HashMap<>();
		queryParameter.put("param_1", matchingKey);

		// evaluate the chained expression
		Object result = chainedMethodExpression.evaluate(inputObjects, queryParameter);

		assertEquals(MockParameter.class, result.getClass());

	}

	@Test
	public void testChained() {

		String matchingKey = "testString";

		// build the message expression
		ParameterReference parameterReference = new ParameterReference();
		parameterReference.varName = "param_1";

		MethodArgumentDeclaration argument = new MethodArgumentDeclaration();
		argument.addParameterReference(parameterReference);

		MethodExpression methodExpression_1 = new MethodExpression();
		methodExpression_1.setMethodName("getParameter");
		methodExpression_1.setMethodArguments(argument);
		
		MethodExpression methodExpression_2 = new MethodExpression();
		methodExpression_2.setMethodName("getType");
		methodExpression_2.setMethodArguments(new MethodArgumentDeclaration());

		// build the chained expression
		ChainedMethodExpression chainedMethodExpression = new ChainedMethodExpression();
		chainedMethodExpression.setObjectKey("a");
		chainedMethodExpression.addMethodExpression(methodExpression_1);
		chainedMethodExpression.addMethodExpression(methodExpression_2);

		// prepare the runtime objects
		MockObject mockObject = new MockObject();
		mockObject.putParameter(matchingKey, new MockParameter());

		Map<String, Object> inputObjects = new HashMap<>();
		inputObjects.put("a", mockObject);

		Map<String, Object> queryParameter = new HashMap<>();
		queryParameter.put("param_1", matchingKey);

		// evaluate the chained expression
		Object result = chainedMethodExpression.evaluate(inputObjects, queryParameter);

		assertEquals("testType", result);

	}

}

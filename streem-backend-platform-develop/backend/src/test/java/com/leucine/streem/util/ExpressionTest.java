package com.leucine.streem.util;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionTest {

  @Test
  public void testComplexExpression() {
    String expression = " (((OW1 - Average)^2 + (OW2 - Average)^2 + (OW3 - Average)^2 + (OW4 - Average)^2 + (OW5 - Average)^2 + (OW6 - Average)^2 + (OW7 - Average)^2 + (OW8 -Average)^2 + (OW9 - Average)^2 + (OW10 - Average)^2) / 9)^0.5";
    Integer precision = 8;
    Set<String> variables = Set.of("OW1", "OW2", "OW3", "OW4", "OW5", "OW6", "OW7", "OW8", "OW9", "OW10", "Average");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 20.00000);
    variableValueMap.put("OW2", 20.00000);
    variableValueMap.put("OW3", 20.00000);
    variableValueMap.put("OW4", 20.00000);
    variableValueMap.put("OW5", 20.00000);
    variableValueMap.put("OW6", 20.00001);
    variableValueMap.put("OW7", 20.00001);
    variableValueMap.put("OW8", 20.00001);
    variableValueMap.put("OW9", 20.00000);
    variableValueMap.put("OW10", 20.00000);
    variableValueMap.put("Average", 20.000003);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("0.00000483", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testAnotherExpression() {
    String expression = " (OW1 + OW2) / 2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 10.0);
    variableValueMap.put("OW2", 20.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("15.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  private String evaluateExpression(String expression, Set<String> variables, Map<String, Double> variableValueMap, Integer precision) {
    Expression e = new ExpressionBuilder(expression)
      .variables(variables)
      .build()
      .setVariables(variableValueMap);
    double temp = e.evaluate();
    return Utility.roundUpDecimalPlaces(temp, precision);
  }
  @Test
  public void testSimpleAddition() {
    String expression = "OW1 + OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 5.0);
    variableValueMap.put("OW2", 3.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("8.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testSimpleSubtraction() {
    String expression = "OW1 - OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 10.0);
    variableValueMap.put("OW2", 3.5);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("6.50", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testSimpleMultiplication() {
    String expression = "OW1 * OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 4.0);
    variableValueMap.put("OW2", 2.5);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("10.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testSimpleDivision() {
    String expression = "OW1 / OW2";
    Integer precision = 3;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 10.0);
    variableValueMap.put("OW2", 4.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("2.500", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testSimpleExponentiation() {
    String expression = "OW1 ^ OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 2.0);
    variableValueMap.put("OW2", 3.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("8.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }
  @Test
  public void testLargeValuesAddition() {
    String expression = "OW1 + OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 1e10);
    variableValueMap.put("OW2", 2e10);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("30000000000.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testLargeValuesSubtraction() {
    String expression = "OW1 - OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 5e10);
    variableValueMap.put("OW2", 3e10);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("20000000000.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testLargeValuesMultiplication() {
    String expression = "OW1 * OW2";
    Integer precision = 2;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 1e5);
    variableValueMap.put("OW2", 2e5);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("20000000000.00", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testLargeValuesDivision() {
    String expression = "OW1 / OW2";
    Integer precision = 5;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 1e10);
    variableValueMap.put("OW2", 3e5);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("33333.33333", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testLargeValuesExponentiation() {
    String expression = "OW1 ^ OW2";
    Integer precision = 0;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 13.3);
    variableValueMap.put("OW2", 2.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("177", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testComplexExpressionWithLargeValues() {
    String expression = "(((OW1 - Average)^2 + (OW2 - Average)^2 + (OW3 - Average)^2) / 2)^0.5";
    Integer precision = 10;
    Set<String> variables = Set.of("OW1", "OW2", "OW3", "Average");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 10.1);
    variableValueMap.put("OW2", 9.4 + 1);
    variableValueMap.put("OW3", 2.4 - 1);
    variableValueMap.put("Average", 1.222);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("9.0301232550", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testNestedExpression() {
    String expression = "(OW1 + OW2) / (OW3 - OW4) + (OW5 * OW6)";
    Integer precision = 5;
    Set<String> variables = Set.of("OW1", "OW2", "OW3", "OW4", "OW5", "OW6");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 13.0);
    variableValueMap.put("OW2", 12.0);
    variableValueMap.put("OW3", 112.2);
    variableValueMap.put("OW4", 11.2);
    variableValueMap.put("OW5", 1.55);
    variableValueMap.put("OW6", 0.005);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("0.25527", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testTrigonometricExpression() {
    String expression = "sin(OW1) + cos(OW2)";
    Integer precision = 8;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", Math.PI / 2);
    variableValueMap.put("OW2", 0.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("2.00000000", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testLogarithmicExpression() {
    String expression = "log(OW1) / log(OW2)";
    Integer precision = 6;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 1e6);
    variableValueMap.put("OW2", 10.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("6.000000", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }

  @Test
  public void testExponentialExpression() {
    String expression = "exp(OW1) + OW2";
    Integer precision = 4;
    Set<String> variables = Set.of("OW1", "OW2");

    Map<String, Double> variableValueMap = new HashMap<>();
    variableValueMap.put("OW1", 8.0);
    variableValueMap.put("OW2", 2.0);

    String result = evaluateExpression(expression, variables, variableValueMap, precision);

    assertEquals("2982.9580", result);
    assertTrue(result.matches("-?\\d+(\\.\\d+)?"), "Result is not in plain string format");
  }
}

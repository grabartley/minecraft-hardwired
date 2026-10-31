package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JunctionRuleTest {

  @ParameterizedTest(name = "{0} with {1} of {2} on is {3}")
  @CsvSource({
    "AND,2,2,true", "AND,1,2,false", "AND,0,0,false",
    "OR,1,2,true", "OR,0,2,false", "OR,0,0,false",
    "NOT,0,1,true", "NOT,1,1,false", "NOT,0,0,false"
  })
  void rulesCombineInputs(JunctionRule rule, int on, int inputs, boolean expected) {
    assertEquals(expected, rule.apply(on, inputs));
  }
}

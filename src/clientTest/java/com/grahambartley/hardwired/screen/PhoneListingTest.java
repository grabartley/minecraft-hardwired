package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneListingTest {

  @ParameterizedTest(name = "number {0} name [{1}]")
  @CsvSource({"105,'',105", "342,'',342"})
  void anUnnamedTelephoneShowsItsNumberAlone(int number, String name, String shown) {
    assertEquals(shown, PhoneListing.label(number, name).getString());
  }

  @ParameterizedTest(name = "number {0} name {1}")
  @CsvSource({"105,Kitchen", "999,Hall"})
  void aNamedTelephoneShowsItsNumberAndName(int number, String name) {
    TranslatableTextContent content =
        (TranslatableTextContent) PhoneListing.label(number, name).getContent();
    assertEquals("screen.hardwired.telephone.listing", content.getKey());
    assertEquals(String.valueOf(number), content.getArgs()[0]);
    assertEquals(name, content.getArgs()[1]);
  }
}

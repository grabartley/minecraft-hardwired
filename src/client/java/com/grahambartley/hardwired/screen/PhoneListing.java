package com.grahambartley.hardwired.screen;

import net.minecraft.text.Text;

final class PhoneListing {

  private PhoneListing() {}

  static Text label(int number, String name) {
    String digits = String.valueOf(number);
    return name.isEmpty()
        ? Text.literal(digits)
        : Text.translatable("screen.hardwired.telephone.listing", digits, name);
  }
}

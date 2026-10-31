package com.grahambartley.hardwired.wire;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record TouchPoint(BlockPos pos, Direction awayFromWire) {}

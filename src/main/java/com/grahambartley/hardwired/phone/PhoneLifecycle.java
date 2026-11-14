package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.device.ElectronicsDriver;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PhoneLifecycle {

  private PhoneLifecycle() {}

  public static void placed(World world, BlockPos anchor, LivingEntity placer, ItemStack stack) {
    if (world instanceof ServerWorld server && placer instanceof ServerPlayerEntity player) {
      Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
      PhoneDirectory.of(server)
          .placedBy(anchor.asLong(), name == null ? "" : name.getString(), player.getUuid());
    }
  }

  public static void listed(World world, BlockPos anchor, List<BlockPos> cells, boolean exchange) {
    if (world instanceof ServerWorld server) {
      PhoneDirectory.of(server)
          .add(anchor.asLong(), cells.stream().map(BlockPos::asLong).toList(), exchange);
    }
  }

  public static void removed(World world, BlockPos anchor) {
    if (world instanceof ServerWorld server) {
      Switchboard.of(server).removed(anchor.asLong());
      PhoneDirectory.of(server).remove(anchor.asLong());
    }
  }

  public static void rewired(ServerWorld world, BlockPos anchor) {
    PhoneDirectory.of(world).markStale();
    ElectronicsDriver.wake(world, anchor);
  }
}

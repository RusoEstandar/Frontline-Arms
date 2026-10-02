package com.frontline.equipment;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Equipamiento lanzable. Clic derecho para lanzar. */
public class GrenadeItem extends Item {
    public enum Kind { FRAG, FLASH, SMOKE }

    public final Kind kind;

    public GrenadeItem(Kind kind) {
        super(new Item.Properties().stacksTo(4));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.8f);
        if (!level.isClientSide) {
            GrenadeEntity g = new GrenadeEntity(level, player);
            g.setItem(new ItemStack(this));
            g.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.1f, 0.0f);
            level.addFreshEntity(g);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}

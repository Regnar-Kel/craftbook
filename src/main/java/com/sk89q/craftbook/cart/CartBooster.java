package com.sk89q.craftbook.cart;

import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

import com.sk89q.craftbook.util.RedstoneUtil.Power;

public class CartBooster extends CartMechanism {

    public CartBooster(double multiplier) {

        super();
        this.multiplier = multiplier;
    }

    private final double multiplier;

    @Override
    public void impact(Minecart cart, CartMechanismBlocks blocks, boolean minor) {
        // validate
        if (cart == null) return;

        // care?
        if (minor) return;

        // enabled?
        if (Power.OFF == isActive(blocks.rail, blocks.base, blocks.sign)) return;

        // speed up or down
        Vector newVelocity;
        if (multiplier > 1) {
            newVelocity = cart.getVelocity().multiply(multiplier);
        } else if (multiplier < 1) {
            newVelocity = cart.getVelocity().multiply(multiplier);
        } else return;
        // go
        cart.setVelocity(newVelocity);
    }

    @Override
    public String getName() {

        return "Booster";
    }

    @Override
    public String[] getApplicableSigns() {

        return null;
    }
}
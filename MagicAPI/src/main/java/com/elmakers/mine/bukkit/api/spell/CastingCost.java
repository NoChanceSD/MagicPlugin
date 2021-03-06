package com.elmakers.mine.bukkit.api.spell;

import com.elmakers.mine.bukkit.api.item.Cost;

/**
 * This reprsents a cost required to cast a Spell.
 * 
 * A Spell may have one or more (or no) CastingCost
 * records assigned to it.
 * 
 * A CastingCost can be XP (or "Mana" when provided by a wand)
 * or Material (reagents) as costs, or a combination of both.
 * 
 */
public interface CastingCost extends Cost {
    /**
     * Returns the raw XP cost.
     *
     * @return The raw XP cost, without reduction.
     */
    public int getXP();

    /**
     * Returns the raw mana cost.
     *
     * @return The raw mana cost, without reduction.
     */
    public int getMana();

    /**
     * Returns the XP amount to deduct
     *
     * @param reducer The CostReducer to use to calculate costs
     * @return The XP amount cost
     */
    public int getXP(CostReducer reducer);

    /**
     * Returns the Mana amount to deduct
     *
     * @param reducer The CostReducer to use to calculate costs
     * @return The Mana amount cost
     */
    public int getMana(CostReducer reducer);

    /**
     * Check to see if a given spell cast should succeed, given that
     * it has any required costs.
     * 
     * @param spell the spell being cast
     * @return true if the caster of the spell has this cost
     */
    public boolean has(Spell spell);

    /**
     * Use this cost, taking it from the caster of the target spell
     * 
     * @param spell the spell being cast
     */
    public void use(Spell spell);
}

package com.elmakers.mine.bukkit.utilities;

import org.bukkit.block.Block;

import com.elmakers.mine.bukkit.plugins.magic.SpellResult;
import com.elmakers.mine.bukkit.plugins.magic.blocks.BlockList;

public class SimpleBlockAction implements BlockAction
{
	protected BlockList blocks = null;

	public SimpleBlockAction()
	{
		blocks = new BlockList();
	}

	public SpellResult perform(Block block)
	{
		blocks.add(block);
		return SpellResult.SUCCESS;
	}

	public BlockList getBlocks()
	{
		return blocks;
	}

}

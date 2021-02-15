package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.ConveyableBlock;
import com.zundrel.conveyance.common.blocks.entities.DoubleMachineBlockEntity;
import com.zundrel.conveyance.common.blocks.entities.SplitterBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class SplitterBlock extends BlockWithEntity implements BlockEntityProvider, ConveyableBlock {
    public SplitterBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new SplitterBlockEntity();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManagerBuilder) {
        stateManagerBuilder.add(Properties.FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext) {
        return this.getDefaultState().with(Properties.FACING, itemPlacementContext.getPlayer().isSneaking() ? itemPlacementContext.getPlayerFacing().getOpposite() : itemPlacementContext.getPlayerFacing());
    }

	@Override
	public void onBlockAdded(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {
		updateDiagonals(world, this, blockPos);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean notify) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof DoubleMachineBlockEntity) {
				ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ((DoubleMachineBlockEntity) blockEntity).getLeftStack());
				ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), ((DoubleMachineBlockEntity) blockEntity).getRightStack());
				((DoubleMachineBlockEntity) blockEntity).setRemoved(true);
			}

			super.onStateReplaced(state, world, pos, newState, notify);
		}

		updateDiagonals(world, this, pos);
	}

	@Override
	public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
		Direction direction = blockState.get(Properties.FACING);
		DoubleMachineBlockEntity machineBlockEntity = (DoubleMachineBlockEntity) world.getBlockEntity(blockPos);

		BlockPos leftPos = blockPos.offset(direction.rotateYCounterclockwise());
		BlockPos rightPos = blockPos.offset(direction.rotateYClockwise());

		BlockEntity leftBlockEntity = world.getBlockEntity(leftPos);
		if (leftBlockEntity instanceof Conveyable && ((Conveyable) leftBlockEntity).validInputSide(direction.rotateYClockwise()))
			machineBlockEntity.setLeft(true);
		else
			machineBlockEntity.setLeft(false);

		BlockEntity rightBlockEntity = world.getBlockEntity(rightPos);
		if (rightBlockEntity instanceof Conveyable && ((Conveyable) rightBlockEntity).validInputSide(direction.rotateYCounterclockwise()))
			machineBlockEntity.setRight(true);
		else
			machineBlockEntity.setRight(false);
	}
}

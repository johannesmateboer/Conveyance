package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.entities.ConveyorBlockEntity;
import com.zundrel.conveyance.common.blocks.entities.DownVerticalConveyorBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class DownVerticalConveyorBlock extends VerticalConveyorBlock {
    public DownVerticalConveyorBlock(Settings settings, int speed) {
        super(settings, speed);

        setDefaultState(getDefaultState().with(ConveyorProperties.FRONT, false).with(ConveyorProperties.CONVEYOR, false));
    }

    @Override
    public ConveyorType getType() {
        return ConveyorType.DOWN_VERTICAL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new DownVerticalConveyorBlockEntity();
    }

	@Override
	public void onBlockAdded(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {
		updateDiagonals(world, this, blockPos.up());
	}

	@Override
    public BlockState getStateForNeighborUpdate(BlockState blockState, Direction fromDirection, BlockState fromState, IWorld world, BlockPos blockPos, BlockPos fromPos) {
        BlockState newState = blockState;
        Direction direction = newState.get(FACING);

        BlockPos frontPos = blockPos.offset(direction.getOpposite());
        BlockPos conveyorPos = blockPos.offset(direction).up();

        BlockEntity frontBlockEntity = world.getBlockEntity(frontPos);
        if (frontBlockEntity instanceof Conveyable && ((Conveyable) frontBlockEntity).validInputSide(direction))
            newState = newState.with(ConveyorProperties.FRONT, true);
        else
            newState = newState.with(ConveyorProperties.FRONT, false);

		BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
        if (world.isAir(blockPos.up()) && conveyorBlockEntity instanceof Conveyable && !((Conveyable) conveyorBlockEntity).hasBeenRemoved() && ((Conveyable) conveyorBlockEntity).isOutputSide(direction.getOpposite(), getType()))
            newState = newState.with(ConveyorProperties.CONVEYOR, true);
        else
            newState = newState.with(ConveyorProperties.CONVEYOR, false);

        return newState;
    }

	@Override
	public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
		Direction direction = blockState.get(FACING);
		ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

		BlockPos downPos = blockPos.down(1);
		BlockPos conveyorPos = blockPos.offset(direction).up();

		BlockEntity downBlockEntity = world.getBlockEntity(downPos);
		if (downBlockEntity instanceof Conveyable && ((Conveyable) downBlockEntity).validInputSide(Direction.UP))
			blockEntity.setDown(true);
		else
			blockEntity.setDown(false);

		BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
		checkForConveyor(world, blockState, conveyorBlockEntity, direction, blockPos, blockPos.up());
	}

	@Override
	public void checkForConveyor(World world, BlockState blockState, BlockEntity conveyorBlockEntity, Direction direction, BlockPos pos, BlockPos upPos) {
		BlockState newState = blockState;

		if (world.isAir(upPos) && conveyorBlockEntity instanceof Conveyable && !((Conveyable) conveyorBlockEntity).hasBeenRemoved() && ((Conveyable) conveyorBlockEntity).isOutputSide(direction.getOpposite(), getType()))
			newState = newState.with(ConveyorProperties.CONVEYOR, true);
		else
			newState = newState.with(ConveyorProperties.CONVEYOR, false);

		world.setBlockState(pos, newState, 8);
	}
}

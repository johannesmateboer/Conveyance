package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorConveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.entities.ConveyorBlockEntity;
import com.zundrel.conveyance.common.items.WrenchItem;
import com.zundrel.conveyance.common.utilities.MovementUtilities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ConveyorBlock extends BlockWithEntity implements BlockEntityProvider, Conveyor {
    private int speed;

    public ConveyorBlock(Settings settings, int speed) {
        super(settings);

        setDefaultState(getDefaultState().with(ConveyorProperties.LEFT, false).with(ConveyorProperties.RIGHT, false).with(ConveyorProperties.UP, false));
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public ConveyorType getType() {
        return ConveyorType.NORMAL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new ConveyorBlockEntity();
    }

    @Override
    public boolean emitsRedstonePower(BlockState blockState) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState blockState, World world, BlockPos blockPos) {
        return ((ConveyorBlockEntity) world.getBlockEntity(blockPos)).isEmpty() ? 0 : 15;
    }

    @Override
    public void onEntityCollision(BlockState blockState, World world, BlockPos blockPos, Entity entity) {
    	BlockPos pos = new BlockPos(entity.getPos());

    	if (!entity.isOnGround() || (entity.getY() - blockPos.getY()) != (4F / 16F))
            return;

        if (entity instanceof PlayerEntity && entity.isSneaking())
            return;

        Direction direction = blockState.get(FACING);

        if (entity instanceof ItemEntity && pos.equals(blockPos) && world.getBlockEntity(blockPos) instanceof ConveyorBlockEntity) {
            ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

            if (blockEntity.isEmpty()) {
                blockEntity.setStack(((ItemEntity) entity).getStack());
                entity.remove();
            }
        } else if (!(entity instanceof ItemEntity)) {
            MovementUtilities.pushEntity(entity, blockPos, 2.0F / getSpeed(), direction);
        }
    }

    @Override
    public void onPlaced(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {
		updateDiagonals(world, this, blockPos);
    }

	@Override
	public void onBlockRemoved(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {
		if (blockState.getBlock() != blockState2.getBlock()) {
			BlockEntity blockEntity_1 = world.getBlockEntity(blockPos);
			if (blockEntity_1 instanceof ConveyorBlockEntity) {
				((ConveyorBlockEntity) blockEntity_1).setRemoved(true);
				ItemScatterer.spawn(world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), ((ConveyorBlockEntity) blockEntity_1).getStack());
				world.updateComparators(blockPos, this);
			}

			super.onBlockRemoved(blockState, world, blockPos, blockState2, boolean_1);
		}

		updateDiagonals(world, this, blockPos);
	}

	@Override
    public BlockState getStateForNeighborUpdate(BlockState blockState, Direction fromDirection, BlockState fromState, World world, BlockPos blockPos, BlockPos fromPos) {
        BlockState newState = blockState;
        Direction direction = newState.get(FACING);

        BlockPos leftPos = blockPos.offset(direction.rotateYCounterclockwise());
        BlockPos rightPos = blockPos.offset(direction.rotateYClockwise());
        BlockPos upPos = blockPos.up();

        BlockEntity leftBlockEntity = world.getBlockEntity(leftPos);
		BlockEntity leftDownBlockEntity = world.getBlockEntity(leftPos.down());
        if (leftBlockEntity instanceof Conveyable && ((Conveyable) leftBlockEntity).isOutputSide(direction.rotateYClockwise(), getType()))
            newState = newState.with(ConveyorProperties.LEFT, true);
        else if (leftDownBlockEntity instanceof ConveyorConveyable && ((ConveyorConveyable) leftDownBlockEntity).getConveyorType() == ConveyorType.VERTICAL && ((ConveyorConveyable) leftDownBlockEntity).isOutputSide(direction.rotateYClockwise(), getType()))
            newState = newState.with(ConveyorProperties.LEFT, true);
        else
            newState = newState.with(ConveyorProperties.LEFT, false);

		BlockEntity rightBlockEntity = world.getBlockEntity(rightPos);
		BlockEntity rightDownBlockEntity = world.getBlockEntity(rightPos.down());
		if (rightBlockEntity instanceof Conveyable && ((Conveyable) rightBlockEntity).isOutputSide(direction.rotateYCounterclockwise(), getType()))
			newState = newState.with(ConveyorProperties.RIGHT, true);
		else if (rightDownBlockEntity instanceof ConveyorConveyable && ((ConveyorConveyable) rightDownBlockEntity).getConveyorType() == ConveyorType.VERTICAL && ((ConveyorConveyable) rightDownBlockEntity).isOutputSide(direction.rotateYCounterclockwise(), getType()))
			newState = newState.with(ConveyorProperties.RIGHT, true);
		else
			newState = newState.with(ConveyorProperties.RIGHT, false);

		BlockEntity upBlockEntity = world.getBlockEntity(upPos);
        if (upBlockEntity instanceof ConveyorConveyable && ((ConveyorConveyable) upBlockEntity).getConveyorType() == ConveyorType.NORMAL)
            newState = newState.with(ConveyorProperties.UP, true);
        else
            newState = newState.with(ConveyorProperties.UP, false);

        return newState;
    }

    @Override
    public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
        Direction direction = blockState.get(FACING);
        ConveyorBlockEntity conveyorBlockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

        BlockPos frontPos = blockPos.offset(direction);

		BlockEntity frontBlockEntity = world.getBlockEntity(blockPos.offset(direction));
        if (frontBlockEntity instanceof Conveyable && ((Conveyable) frontBlockEntity).validInputSide(direction.getOpposite()))
            conveyorBlockEntity.setFront(true);
        else
            conveyorBlockEntity.setFront(false);

		BlockEntity frontAcrossBlockEntity = world.getBlockEntity(blockPos.offset(direction).offset(direction));
		if (frontBlockEntity instanceof ConveyorConveyable && ((ConveyorConveyable) frontBlockEntity).validInputSide(direction.getOpposite()) && ((ConveyorConveyable) frontBlockEntity).validInputSide(direction) && frontAcrossBlockEntity instanceof ConveyorConveyable && world.getBlockState(blockPos.offset(direction).offset(direction)).get(HorizontalFacingBlock.FACING) == direction.getOpposite())
			conveyorBlockEntity.setAcross(true);
		else
			conveyorBlockEntity.setAcross(false);

		BlockEntity downBlockEntity = world.getBlockEntity(blockPos.offset(direction).down());
        if (downBlockEntity instanceof Conveyable && ((Conveyable) downBlockEntity).validInputSide(Direction.UP))
            conveyorBlockEntity.setDown(true);
        else
            conveyorBlockEntity.setDown(false);
    }

    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos blockPos, PlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult) {
        ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

        if (!playerEntity.getStackInHand(hand).isEmpty() && (Block.getBlockFromItem(playerEntity.getStackInHand(hand).getItem()) instanceof Conveyor || playerEntity.getStackInHand(hand).getItem() instanceof WrenchItem)) {
            return ActionResult.PASS;
        } else if (!playerEntity.getStackInHand(hand).isEmpty() && blockEntity.isEmpty()) {
            blockEntity.setStack(playerEntity.getStackInHand(hand));
            playerEntity.setStackInHand(hand, ItemStack.EMPTY);

            return ActionResult.SUCCESS;
        } else if (!blockEntity.isEmpty()) {
            playerEntity.inventory.offerOrDrop(world, blockEntity.getStack());
            blockEntity.removeStack();

            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManagerBuilder) {
        stateManagerBuilder.add(FACING, ConveyorProperties.LEFT, ConveyorProperties.RIGHT, ConveyorProperties.UP);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext) {
    	World world = itemPlacementContext.getWorld();
        BlockPos blockPos = itemPlacementContext.getBlockPos();
    	BlockState newState = this.getDefaultState().with(FACING, itemPlacementContext.getPlayer().isSneaking() ? itemPlacementContext.getPlayerFacing().getOpposite() : itemPlacementContext.getPlayerFacing());

    	newState = newState.getStateForNeighborUpdate(null, newState, world, blockPos, blockPos);

		return newState;
    }

    @Override
    public boolean isTranslucent(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1) {
        return false;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState blockState, BlockView blockView, BlockPos blockPos, ShapeContext entityContext) {
        VoxelShape conveyor = VoxelShapes.cuboid(0, 0, 0, 1, (4F / 16F), 1);
        if (blockState.get(ConveyorProperties.UP)) {
            return VoxelShapes.fullCube();
        }
        return conveyor;
    }
}

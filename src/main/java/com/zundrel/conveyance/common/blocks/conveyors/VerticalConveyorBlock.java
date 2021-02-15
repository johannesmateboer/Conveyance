package com.zundrel.conveyance.common.blocks.conveyors;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.entities.ConveyorBlockEntity;
import com.zundrel.conveyance.common.blocks.entities.VerticalConveyorBlockEntity;
import com.zundrel.conveyance.common.utilities.RotationUtilities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class VerticalConveyorBlock extends BlockWithEntity implements BlockEntityProvider, Conveyor {
    private int speed;

    public VerticalConveyorBlock(Settings settings, int speed) {
        super(settings);

        setDefaultState(getDefaultState()
                .with(ConveyorProperties.FRONT, false)
                .with(ConveyorProperties.CONVEYOR, false)
        );
        this.speed = speed;
    }

    @Override
    public int getSpeed() {
        return this.speed;
    }

    @Override
    public ConveyorType getType() {
        return ConveyorType.VERTICAL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new VerticalConveyorBlockEntity();
    }

    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos blockPos, PlayerEntity playerEntity, Hand hand, BlockHitResult blockHitResult) {
        ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

        if (!playerEntity.getStackInHand(hand).isEmpty() && Block.getBlockFromItem(playerEntity.getStackInHand(hand).getItem()) instanceof Conveyor) {
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
    public void onStateReplaced(BlockState blockState, World world, BlockPos blockPos, BlockState blockState2, boolean boolean_1) {

        if (blockState.getBlock() != blockState2.getBlock()) {
            BlockEntity blockEntity_1 = world.getBlockEntity(blockPos);
            if (blockEntity_1 instanceof VerticalConveyorBlockEntity) {
                ((VerticalConveyorBlockEntity) blockEntity_1).setRemoved(true);
                ItemScatterer.spawn(world, blockPos.getX(), blockPos.getY(), blockPos.getZ(), ((VerticalConveyorBlockEntity) blockEntity_1).getStack());
                world.updateComparators(blockPos, this);
            }
            super.onStateReplaced(blockState, world, blockPos, blockState2, boolean_1);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState originalState, Direction direction, BlockState newState, WorldAccess world, BlockPos blockPos, BlockPos posFrom) {
        if (newState.isAir()) {
            return originalState;
        }

        direction = originalState.get(Properties.FACING);

        BlockPos frontPos = blockPos.offset(direction.getOpposite());
        BlockPos upPos = blockPos.up();
        BlockPos conveyorPos = blockPos.offset(direction).up();

        BlockEntity frontBlockEntity = world.getBlockEntity(frontPos);
        if (frontBlockEntity instanceof Conveyable && ((Conveyable) frontBlockEntity).isOutputSide(direction, getType())) {
            originalState = originalState.with(ConveyorProperties.FRONT, true);
        } else
            originalState = originalState.with(ConveyorProperties.FRONT, false);

        BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
        if (world.isAir(upPos) &&
                conveyorBlockEntity instanceof Conveyable &&
                !((Conveyable) conveyorBlockEntity).hasBeenRemoved() &&
                ((Conveyable) conveyorBlockEntity).validInputSide(direction.getOpposite()))
            originalState = originalState.with(ConveyorProperties.CONVEYOR, true);
        else
            originalState = originalState.with(ConveyorProperties.CONVEYOR, false);

        return originalState;
    }

    @Override
    public void neighborUpdate(BlockState blockState, World world, BlockPos blockPos, Block block, BlockPos blockPos2, boolean boolean_1) {
        setInputs(blockState, world, blockPos);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        setInputs(state, world, pos);
        checkForConveyor(world, state, world.getBlockEntity(pos), state.get(Properties.FACING), pos, pos.up());
    }

    /**
     * Sets the proper inputs
     *
     * @param blockState Blockstate of the conveyor
     * @param world      World-reference
     * @param blockPos   Block position-reference
     */
    public void setInputs(BlockState blockState, World world, BlockPos blockPos) {
        Direction direction = blockState.get(Properties.FACING);
        ConveyorBlockEntity blockEntity = (ConveyorBlockEntity) world.getBlockEntity(blockPos);

        BlockPos upPos = blockPos.up();
        BlockPos conveyorPos = blockPos.offset(direction).up();

        BlockEntity upBlockEntity = world.getBlockEntity(upPos);
        if (upBlockEntity instanceof Conveyable && ((Conveyable) upBlockEntity).validInputSide(Direction.DOWN)) {
            ((VerticalConveyorBlockEntity) blockEntity).setUp(true);
        } else {
            ((VerticalConveyorBlockEntity) blockEntity).setUp(false);
        }

        BlockEntity conveyorBlockEntity = world.getBlockEntity(conveyorPos);
        checkForConveyor(world, blockState, conveyorBlockEntity, direction, blockPos, conveyorPos); //was uppos
    }

    public void checkForConveyor(World world, BlockState blockState, BlockEntity conveyorBlockEntity, Direction direction, BlockPos pos, BlockPos upPos) {
        BlockState newState = blockState;

        if (
                world.isAir(upPos) &&
                        conveyorBlockEntity instanceof Conveyable &&
                        !((Conveyable) conveyorBlockEntity).hasBeenRemoved() &&
                        ((Conveyable) conveyorBlockEntity).validInputSide(direction.getOpposite())
        )
            newState = newState.with(ConveyorProperties.CONVEYOR, true);
        else
            newState = newState.with(ConveyorProperties.CONVEYOR, false);

        world.setBlockState(pos, newState, 8);
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManagerBuilder) {
        stateManagerBuilder.add(Properties.FACING, ConveyorProperties.FRONT, ConveyorProperties.CONVEYOR);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext itemPlacementContext) {
        World world = itemPlacementContext.getWorld();
        BlockPos blockPos = itemPlacementContext.getBlockPos();
        BlockState newState = this.getDefaultState().with(Properties.FACING, itemPlacementContext.getPlayerFacing());

        newState = newState.getStateForNeighborUpdate(newState.get(Properties.FACING), newState, world, blockPos, blockPos);

        return newState;
    }

    @Override
    public boolean isTranslucent(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1) {
        return true;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState blockState, BlockView blockView, BlockPos blockPos, ShapeContext entityContext) {
        VoxelShape box1 = RotationUtilities.getRotatedShape(new Box(0, 0, 0, 1, 1, (4F / 16F)), blockState.get(Properties.FACING));
        VoxelShape box2 = RotationUtilities.getRotatedShape(new Box(0, 0, 0, 1, (4F / 16F), 1), blockState.get(Properties.FACING));

        if (blockState.get(ConveyorProperties.FRONT)) {
            return VoxelShapes.union(box1, box2);
        } else {
            return box1;
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }
}

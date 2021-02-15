package com.zundrel.conveyance.common.blocks.decor;

import com.zundrel.conveyance.common.blocks.conveyors.ConveyorProperties;
import com.zundrel.conveyance.mixin.EntityShapeContextAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

@SuppressWarnings("deprecation")
public class CatwalkBlock extends Block {
    public CatwalkBlock(Settings settings) {
        super(settings);

        setDefaultState(this.getDefaultState()
                .with(ConveyorProperties.FLOOR, true)
                .with(Properties.NORTH, false)
                .with(Properties.EAST, false)
                .with(Properties.SOUTH, false)
                .with(Properties.WEST, false)
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ConveyorProperties.FLOOR, Properties.NORTH, Properties.EAST, Properties.SOUTH, Properties.WEST);
    }

    public BooleanProperty getPropertyFromDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return Properties.NORTH;
            case EAST:
                return Properties.EAST;
            case SOUTH:
                return Properties.SOUTH;
            case WEST:
                return Properties.WEST;
            default:
                return null;
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction facing, BlockState newState, WorldAccess world, BlockPos blockPos, BlockPos posFrom) {
        boolean neighborSameType = newState.getBlock() instanceof CatwalkBlock;

        if (facing == Direction.NORTH)
            newState = newState.with(Properties.NORTH, neighborSameType);
        if (facing == Direction.EAST)
            newState = newState.with(Properties.EAST, neighborSameType);
        if (facing == Direction.SOUTH)
            newState = newState.with(Properties.SOUTH, neighborSameType);
        if (facing == Direction.WEST)
            newState = newState.with(Properties.WEST, neighborSameType);

        return newState;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        Box bottom = new Box(0, 0, 0, 1, (1F / 16F), 1);
        Box north = new Box(0, 0, 0, 1, 1, (1F / 16F));
        Box east = new Box((15F / 16F), 0, 0, 1, 1, 1);
        Box south = new Box(0, 0, (15F / 16F), 1, 1, 1);
        Box west = new Box(0, 0, 0, (1F / 16F), 1, 1);
        VoxelShape fullShape = VoxelShapes.union(VoxelShapes.cuboid(bottom), VoxelShapes.cuboid(north), VoxelShapes.cuboid(east), VoxelShapes.cuboid(south), VoxelShapes.cuboid(west));

        if (context instanceof EntityShapeContext) {
            Item heldItem = ((EntityShapeContextAccess) context).getHeldItem();
        }

        return getCollisionShape(state, view, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ePos) {
        Box bottom = new Box(0, 0, 0, 1, (1F / 16F), 1);
        Box north = new Box(0, 0, 0, 1, 1, (1F / 16F));
        Box east = new Box((15F / 16F), 0, 0, 1, 1, 1);
        Box south = new Box(0, 0, (15F / 16F), 1, 1, 1);
        Box west = new Box(0, 0, 0, (1F / 16F), 1, 1);
        VoxelShape shape = VoxelShapes.empty();

        if (state.get(ConveyorProperties.FLOOR))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(bottom));
        if (!state.get(Properties.NORTH))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(north));
        if (!state.get(Properties.EAST))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(east));
        if (!state.get(Properties.SOUTH))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(south));
        if (!state.get(Properties.WEST))
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(west));

        return shape;
    }
}

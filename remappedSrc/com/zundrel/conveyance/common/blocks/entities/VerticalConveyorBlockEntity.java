package com.zundrel.conveyance.common.blocks.entities;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorConveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.blocks.conveyors.ConveyorProperties;
import com.zundrel.conveyance.common.registries.ConveyanceBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VerticalConveyorBlockEntity extends ConveyorBlockEntity {
    protected boolean up = false;
    protected int horizontalPosition;
    protected int prevHorizontalPosition;

    public VerticalConveyorBlockEntity() {
        super(ConveyanceBlockEntities.VERTICAL_CONVEYOR);
    }

    public VerticalConveyorBlockEntity(BlockEntityType type) {
        super(type);
    }

    @Override
    public void tick() {
        Direction direction = getCachedState().get(HorizontalFacingBlock.FACING);
		int speed = ((Conveyor) getCachedState().getBlock()).getSpeed();

		if (!isEmpty()) {
			if (getCachedState().get(ConveyorProperties.CONVEYOR)) {
				BlockPos conveyorPos = getPos().offset(direction).up();
				if (getWorld().getBlockEntity(conveyorPos) instanceof Conveyable) {
					Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(conveyorPos);
					if (position < speed) {
						handleMovement(conveyable, speed, false);
					} else {
						prevPosition = speed;
						handleMovementHorizontal(conveyable, speed, true);
					}
				}
			} else if (up) {
				BlockPos upPos = getPos().up();
				if (getWorld().getBlockEntity(upPos) instanceof Conveyable) {
					Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(upPos);
					handleMovement(conveyable, speed, true);
				}
			} else {
				setPosition(0);
			}
		} else {
			setPosition(0);
		}
    }

	public void handleMovementHorizontal(Conveyable conveyable, int speed, boolean transition) {
		if (conveyable.accepts(getStack())) {
			if (horizontalPosition < speed) {
				setHorizontalPosition(getHorizontalPosition() + 2);
			} else if (transition && !getWorld().isClient() && horizontalPosition >= speed) {
				conveyable.give(getStack());
				removeStack();
			}
		} else if (conveyable instanceof ConveyorConveyable) {
			ConveyorConveyable conveyor = (ConveyorConveyable) conveyable;

			if (horizontalPosition < speed && horizontalPosition + 4 < conveyor.getPosition() && conveyor.getPosition() > 4) {
				setHorizontalPosition(getHorizontalPosition() + 2);
			} else {
				prevHorizontalPosition = horizontalPosition;
			}
		}
	}

	@Override
	public boolean validInputSide(Direction direction) {
		return !getCachedState().get(ConveyorProperties.FRONT) && direction == Direction.DOWN || direction == getCachedState().get(HorizontalFacingBlock.FACING).getOpposite();
	}

	@Override
	public boolean isOutputSide(Direction direction, ConveyorType type) {
		return type == ConveyorType.NORMAL ? getCachedState().get(HorizontalFacingBlock.FACING) == direction : direction == Direction.UP;
	}

    @Override
    public ItemStack removeStack() {
        horizontalPosition = 0;
        prevHorizontalPosition = 0;
        return super.removeStack();
    }

    public boolean hasUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
        markDirty();
    }

    @Override
    public int[] getRenderAttachmentData() {
        return new int[] { position, prevPosition, horizontalPosition, prevHorizontalPosition };
    }

    public int getHorizontalPosition() {
        return horizontalPosition;
    }

    public void setHorizontalPosition(int horizontalPosition) {
        if (horizontalPosition == 0)
            this.prevHorizontalPosition = 0;
        else
            this.prevHorizontalPosition = this.horizontalPosition;

        this.horizontalPosition = horizontalPosition;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag compoundTag) {
        super.fromTag(state, compoundTag);
        up = compoundTag.getBoolean("up");
        horizontalPosition = compoundTag.getInt("horizontalPosition");
        prevHorizontalPosition = horizontalPosition = compoundTag.getInt("horizontalPosition");
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        fromTag(getCachedState(), compoundTag);
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        compoundTag.putBoolean("up", up);
        compoundTag.putInt("horizontalPosition", horizontalPosition);
        return super.toTag(compoundTag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return toTag(compoundTag);
    }
}

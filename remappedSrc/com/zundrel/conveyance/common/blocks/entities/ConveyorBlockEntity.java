package com.zundrel.conveyance.common.blocks.entities;

import com.zundrel.conveyance.api.Conveyable;
import com.zundrel.conveyance.api.Conveyor;
import com.zundrel.conveyance.api.ConveyorConveyable;
import com.zundrel.conveyance.api.ConveyorType;
import com.zundrel.conveyance.common.inventory.SingularStackInventory;
import com.zundrel.conveyance.common.registries.ConveyanceBlockEntities;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ConveyorBlockEntity extends BlockEntity implements ConveyorConveyable, SingularStackInventory, BlockEntityClientSerializable, RenderAttachmentBlockEntity, Tickable {
    private DefaultedList<ItemStack> stacks = DefaultedList.ofSize(1, ItemStack.EMPTY);
    protected boolean front = false;
    protected boolean down = false;
    protected boolean across = false;
    protected int position = 0;
    protected int prevPosition = 0;
    protected boolean hasBeenRemoved = false;

    public ConveyorBlockEntity() {
        super(ConveyanceBlockEntities.CONVEYOR);
    }

    public ConveyorBlockEntity(BlockEntityType type) {
        super(type);
    }

    @Override
    public void tick() {
		Direction direction = getCachedState().get(HorizontalFacingBlock.FACING);
        int speed = ((Conveyor) getCachedState().getBlock()).getSpeed();

		if (!isEmpty()) {
			if (across) {
				BlockPos frontPos = getPos().offset(direction);
				BlockPos frontAcrossPos = frontPos.offset(direction);
				if (getWorld().getBlockEntity(frontPos) instanceof ConveyorConveyable && getWorld().getBlockEntity(frontAcrossPos) instanceof ConveyorConveyable) {
					Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(frontPos);
					Conveyable acrossConveyable = (Conveyable) getWorld().getBlockEntity(frontAcrossPos);
					handleMovementAcross(conveyable, acrossConveyable, speed, true);
				}
			} else if (front) {
				BlockPos frontPos = getPos().offset(direction);
				if (getWorld().getBlockEntity(frontPos) instanceof Conveyable) {
					Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(frontPos);
					handleMovement(conveyable, speed, true);
				}
			} else if (down) {
				BlockPos downPos = getPos().offset(direction).down();
				if (getWorld().getBlockEntity(downPos) instanceof Conveyable) {
					Conveyable conveyable = (Conveyable) getWorld().getBlockEntity(downPos);
					handleMovement(conveyable, speed, true);
				}
			} else {
				setPosition(0);
			}
		} else {
			setPosition(0);
		}
    }

    public void handleMovement(Conveyable conveyable, int speed, boolean transition) {
		if (conveyable.accepts(getStack())) {
			if (position < speed) {
				setPosition(getPosition() + 1);
			} else if (transition && !getWorld().isClient() && position >= speed) {
				conveyable.give(getStack());
				removeStack();
			}
		} else if (conveyable instanceof ConveyorConveyable) {
			ConveyorConveyable conveyor = (ConveyorConveyable) conveyable;

			if (position < speed && position + 4 < conveyor.getPosition() && conveyor.getPosition() > 4) {
				setPosition(getPosition() + 1);
			} else {
				prevPosition = position;
			}
		}
	}

	public void handleMovementAcross(Conveyable conveyable, Conveyable acrossConveyable, int speed, boolean transition) {
		if (conveyable.accepts(getStack())) {
			if (position < speed) {
				if (conveyable instanceof ConveyorConveyable && acrossConveyable instanceof ConveyorConveyable) {
					ConveyorConveyable conveyor = (ConveyorConveyable) conveyable;
					ConveyorConveyable acrossConveyor = (ConveyorConveyable) acrossConveyable;

					if (position < speed && acrossConveyor.getPosition() == 0) {
						setPosition(getPosition() + 1);
					} else {
						prevPosition = position;
					}
				}
			} else if (transition && !getWorld().isClient() && position >= speed) {
				conveyable.give(getStack());
				removeStack();
			}
		} else if (conveyable instanceof ConveyorConveyable && acrossConveyable instanceof ConveyorConveyable) {
			ConveyorConveyable conveyor = (ConveyorConveyable) conveyable;
			ConveyorConveyable acrossConveyor = (ConveyorConveyable) acrossConveyable;

			if (position < speed && acrossConveyor.getPosition() == 0 && position + 4 < conveyor.getPosition() && conveyor.getPosition() > 4) {
				setPosition(getPosition() + 1);
			} else {
				prevPosition = position;
			}
		}
	}

	@Override
	public boolean hasBeenRemoved() {
		return hasBeenRemoved;
	}

	@Override
	public void setRemoved(boolean hasBeenRemoved) {
		this.hasBeenRemoved = hasBeenRemoved;
	}

	@Override
	public ConveyorType getConveyorType() {
		return ((Conveyor) getCachedState().getBlock()).getType();
	}

	@Override
	public boolean accepts(ItemStack stack) {
		return isEmpty();
	}

	@Override
	public boolean validInputSide(Direction direction) {
		return direction != getCachedState().get(HorizontalFacingBlock.FACING) && direction != Direction.UP && direction != Direction.DOWN;
	}

	@Override
	public boolean isOutputSide(Direction direction, ConveyorType type) {
		return getCachedState().get(HorizontalFacingBlock.FACING) == direction;
	}

	@Override
	public void give(ItemStack stack) {
		setStack(stack);
	}

	@Override
    public DefaultedList<ItemStack> getItems() {
        return stacks;
    }

	@Override
	public int size() {
		return 1;
	}

	@Override
    public ItemStack removeStack() {
        position = 0;
        prevPosition = 0;
        return SingularStackInventory.super.removeStack();
    }

    @Override
    public int[] getRenderAttachmentData() {
        return new int[] { position, prevPosition };
    }

    public boolean hasFront() {
        return front;
    }

    public void setFront(boolean front) {
        this.front = front;
        markDirty();
    }

    public boolean hasDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
        markDirty();
    }

	public boolean hasAcross() {
		return across;
	}

	public void setAcross(boolean across) {
		this.across = across;
	}

	@Override
    public int getPosition() {
        return position;
    }

    @Override
    public int getPrevPosition() {
        return prevPosition;
    }

    public void setPosition(int position) {
        if (position == 0)
            this.prevPosition = 0;
        else
            this.prevPosition = this.position;
        this.position = position;
    }

    public void sync() {
        if (world instanceof ServerWorld) {
            ((ServerWorld)world).getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        sync();
    }

	@Override
    public void fromTag(BlockState state, CompoundTag compoundTag) {
        super.fromTag(state, compoundTag);
        clear();
        setStack(ItemStack.fromTag(compoundTag.getCompound("stack")));
        front = compoundTag.getBoolean("front");
        down = compoundTag.getBoolean("down");
        across = compoundTag.getBoolean("across");
        position = compoundTag.getInt("position");
        prevPosition = compoundTag.getInt("position");
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        fromTag(getCachedState(), compoundTag);
    }

    @Override
    public CompoundTag toTag(CompoundTag compoundTag) {
        compoundTag.put("stack", getStack().toTag(new CompoundTag()));
        compoundTag.putBoolean("front", front);
        compoundTag.putBoolean("down", down);
        compoundTag.putBoolean("across", across);
        compoundTag.putInt("position", position);
        return super.toTag(compoundTag);
    }

    @Override
    public CompoundTag toInitialChunkDataTag() {
        return toTag(new CompoundTag());
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return toTag(compoundTag);
    }
}

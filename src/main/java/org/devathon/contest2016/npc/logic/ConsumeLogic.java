/*
 * MIT License
 * 
 * Copyright (c) 2016
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.devathon.contest2016.npc.logic;

import org.bukkit.inventory.ItemStack;
import org.devathon.contest2016.npc.NPCController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Cryptkeeper
 * @since 05.11.2016
 */
abstract class ConsumeLogic implements Logic {

    protected final NPCController npc;

    ConsumeLogic(NPCController npc) {
        this.npc = npc;
    }

    @Override
    public void execute() {
        List<ItemStack> itemStacks = getRelevantItemStacks();

        Collections.shuffle(itemStacks);

        ItemStack toRemove = itemStacks.get(0);

        if (toRemove.getAmount() == 1) {
            itemStacks.remove(0);
        } else {
            toRemove.setAmount(toRemove.getAmount() - 1);
        }

        npc.getInventory().remove(toRemove);

        _execute(toRemove);
    }

    protected abstract void _execute(ItemStack itemStack);

    protected abstract boolean isRelevant(ItemStack itemStack);

    List<ItemStack> getRelevantItemStacks() {
        List<ItemStack> itemStacks = null;

        for (ItemStack itemStack : npc.getInventory()) {
            if (isRelevant(itemStack)) {
                if (itemStacks == null) {
                    itemStacks = new ArrayList<>();
                }

                itemStacks.add(itemStack);
            }
        }

        return itemStacks;
    }
}

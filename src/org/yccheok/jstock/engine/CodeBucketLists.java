/*
 * JStock - Free Stock Market Software
 * Copyright (C) 2014 Yan Cheng Cheok <yccheok@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.yccheok.jstock.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.yccheok.jstock.gui.Pair;

/**
 *
 * @author yccheok
 */
public class CodeBucketLists {
    public CodeBucketLists(int maxBucketSize) {
        if (maxBucketSize <= 0) {
            throw new java.lang.IllegalArgumentException();
        }
        
        this.maxBucketSize = maxBucketSize;
    }
    
    public boolean add(Code code) {
        final String id = getStockServerFactoriesId(code);
        
        BucketList<Code> bucketList = bucketLists.get(id);
        
        if (bucketList == null) {
            bucketList = new BucketList(this.maxBucketSize);
            bucketLists.put(id, bucketList);
        }
        
        final int beforeSize = bucketList.size();
        
        final boolean status = bucketList.add(code);
        if (status == false) {
            return status;
        }
        
        final int afterSize = bucketList.size();
        
        assert(afterSize >= 1);
        assert(afterSize >= beforeSize);
        
        if (afterSize == beforeSize) {
            return true;
        }
        
        Integer idsIndex = basedIndexInfosIndexMapping.get(id);
        if (idsIndex == null) {
            int basedIndexInfosSize = basedIndexInfos.size();
            basedIndexInfosIndexMapping.put(id, basedIndexInfosSize);            
            
            if (basedIndexInfosSize == 0) {
                basedIndexInfos.add(Pair.create(id, 0));
            } else {
                Pair<String, Integer> previousBasedIndexInfo = basedIndexInfos.get(basedIndexInfosSize - 1);
                BucketList<Code> previousBucketList = bucketLists.get(previousBasedIndexInfo.first);
                int previousBucketListSize = previousBucketList.size();
                int myBasedIndex = previousBasedIndexInfo.second + previousBucketListSize;
                
                basedIndexInfos.add(Pair.create(id, myBasedIndex));
            }            
        } else {
            for (int i = (idsIndex + 1), ei = basedIndexInfos.size(); i < ei; i++) {
                Pair<String, Integer> basedIndex = basedIndexInfos.get(i);
                basedIndexInfos.set(i, Pair.create(basedIndex.first, basedIndex.second + 1));
            }
        }
        
        bucketListsIndexMapping.add(afterSize - 1, id);
        
        return true;
    }
    
    private String getStockServerFactoriesId(Code code) {
        List<StockServerFactory> stockServerFactories = Factories.INSTANCE.getStockServerFactories(code);
        
        StringBuilder stringBuilder = new StringBuilder();
        for (StockServerFactory stockServerFactory : stockServerFactories) {
            // Get simple name will be good enough at this moment
            stringBuilder.append(stockServerFactory.getClass().getSimpleName());
        }
        return stringBuilder.toString();
    }
    
    private final int maxBucketSize;
    
    /*        -----------------
     * "A" => | 0 | 1 | 2 | 3 |
     *        -----------------
     * "B" => | 4 |
     *        ---------
     * "C" => | 5 | 6 | 
     *        --------- 
     */
    private final Map<String, BucketList<Code>> bucketLists = new ConcurrentHashMap<String, BucketList<Code>>();
    
    /*
     * -------------------------------------------
     * | "A" | "A" | "A" | "A" | "B" | "C" | "C" |
     * -------------------------------------------
     */
    private final List<String> bucketListsIndexMapping = new java.util.concurrent.CopyOnWriteArrayList<String>();
    
    /*        
     * "A" => 1
     * "B" => 2
     * "C" => 3
     */    
    private final Map<String, Integer> basedIndexInfosIndexMapping = new ConcurrentHashMap<String, Integer>();
    
    /*
     * ---------------------------------
     * | ("A", 0) | ("B", 4), ("C", 5) |
     * ---------------------------------
     */
    private final List<Pair<String, Integer>> basedIndexInfos = new java.util.concurrent.CopyOnWriteArrayList<Pair<String, Integer>>();
}
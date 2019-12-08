package net.darkhax.botanypots.crop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.darkhax.bookshelf.Bookshelf;
import net.darkhax.bookshelf.util.MCJsonUtils;
import net.darkhax.botanypots.BotanyPots;
import net.darkhax.botanypots.PacketUtils;
import net.darkhax.botanypots.soil.SoilInfo;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class CropInfo {
    
    /**
     * The id of the crop.
     */
    private final ResourceLocation id;
    
    /**
     * The ingredient used for the crop's seed.
     */
    private Ingredient seed;
    
    /**
     * An array of valid soil categories.
     */
    private Set<String> soilCategories;
    
    /**
     * The crop's growth tick factor.
     */
    private int growthTicks;
    
    /**
     * The crop's growth modifier.
     */
    private float growthMultiplier;
    
    /**
     * An array of things the crop can drop.
     */
    private List<HarvestEntry> results;
    
    /**
     * The BlockState to render for the crop.
     */
    private BlockState displayBlock;
    
    public CropInfo(ResourceLocation id, Ingredient seed, Set<String> soilCategories, int growthTicks, float growthMultiplier, List<HarvestEntry> results, BlockState displayState) {
        
        this.id = id;
        this.seed = seed;
        this.soilCategories = soilCategories;
        this.growthTicks = growthTicks;
        this.growthMultiplier = growthMultiplier;
        this.results = results;
        this.displayBlock = displayState;
    }
    
    /**
     * Gets the internal ID of the crop.
     * 
     * @return The internal ID of the crop.
     */
    public ResourceLocation getId () {
        
        return this.id;
    }
    
    /**
     * Gets an ingredient that can be used to match an ItemStack as a seed for this crop.
     * 
     * @return An ingredient that can used to match an ItemStack as a seed for the crop.
     */
    public Ingredient getSeed () {
        
        return this.seed;
    }
    
    /**
     * Gets all the soil categories that are valid for this crop.
     * 
     * @return An array of valid soil categories for this crop.
     */
    public Set<String> getSoilCategories () {
        
        return this.soilCategories;
    }
    
    /**
     * Gets all the possible results when harvesting the crop.
     * 
     * @return An array of harvest results for the crop.
     */
    public List<HarvestEntry> getResults () {
        
        return this.results;
    }
    
    /**
     * Gets the state to render when displaying the crop.
     * 
     * @return The state to display when rendering the crop.
     */
    public BlockState getDisplayState () {
        
        return this.displayBlock;
    }
    
    /**
     * Deserializes a CropInfo from a JsonObject.
     * 
     * @param id The id to assign to the CropInfo.
     * @param json The object to read all the properties from.
     * @return The deserialized CropInfo.
     */
    public static CropInfo deserialize (ResourceLocation id, JsonObject json) {
        
        final Ingredient seed = Ingredient.deserialize(json.getAsJsonObject("seed"));
        final Set<String> validSoils = deserializeSoilInfo(id, json);
        final int growthTicks = JSONUtils.getInt(json, "growthTicks");
        final float growthModifier = JSONUtils.getFloat(json, "growthModifier");
        final List<HarvestEntry> results = deserializeCropEntries(id, json);
        final BlockState displayState = MCJsonUtils.deserializeBlockState(json.getAsJsonObject("display"));
        return new CropInfo(id, seed, validSoils, growthTicks, growthModifier, results, displayState);
    }
    
    public static CropInfo deserialize (PacketBuffer buf) {
        
        final ResourceLocation id = buf.readResourceLocation();
        final Ingredient seed = Ingredient.read(buf);
        final Set<String> validSoils = new HashSet<>();
        PacketUtils.deserializeStringCollection(buf, validSoils);
        final int growthTicks = buf.readInt();
        final float growthModifier = buf.readFloat();
        final List<HarvestEntry> results = new ArrayList<>();
        
        // for (int i = 0; i < buf.readInt(); i++) {
        //
        // results.add(HarvestEntry.deserialize(buf));
        // }
        
        final BlockState displayState = PacketUtils.deserializeBlockState(buf);
        return new CropInfo(id, seed, validSoils, growthTicks, growthModifier, results, displayState);
    }
    
    public static void serialize (PacketBuffer buffer, CropInfo info) {
        
        buffer.writeResourceLocation(info.getId());
        info.getSeed().write(buffer);
        PacketUtils.serializeStringCollection(buffer, info.getSoilCategories());
        buffer.writeInt(info.getGrowthTicks());
        buffer.writeFloat(info.getGrowthMultiplier());
        
        // buffer.writeInt(info.getResults().size());
        //
        // for (final HarvestEntry entry : info.getResults()) {
        //
        // HarvestEntry.serialize(buffer, entry);
        // }
        
        PacketUtils.serializeBlockState(buffer, info.getDisplayState());
    }
    
    /**
     * A helper method to deserialize soil categories from an array.
     * 
     * @param ownerId The Id of the SoilInfo currently being deserialized.
     * @param json The JsonObject to read from.
     * @return A set of soil categories.
     */
    private static Set<String> deserializeSoilInfo (ResourceLocation ownerId, JsonObject json) {
        
        final Set<String> categories = new HashSet<>();
        
        for (final JsonElement element : json.getAsJsonArray("categories")) {
            
            categories.add(element.getAsString().toLowerCase());
        }
        
        return categories;
    }
    
    /**
     * A helper method for reading crop harvest entries.
     * 
     * @param ownerId The id of the CropInfo being deserialized.
     * @param json The json data to read from.
     * @return A list of crop harvest entries.
     */
    private static List<HarvestEntry> deserializeCropEntries (ResourceLocation ownerId, JsonObject json) {
        
        final List<HarvestEntry> crops = new ArrayList<>();
        
        for (final JsonElement entry : json.getAsJsonArray("results")) {
            
            if (!entry.isJsonObject()) {
                
                BotanyPots.LOGGER.error("Crop entry in {} is not a JsonObject.", ownerId);
            }
            
            else {
                
                final HarvestEntry cropEntry = HarvestEntry.deserialize(entry.getAsJsonObject());
                crops.add(cropEntry);
            }
        }
        
        return crops;
    }
    
    /**
     * Gets the growth tick factor for the crop.
     * 
     * @return The crop's growth tick factor.
     */
    public int getGrowthTicks () {
        
        return this.growthTicks;
    }
    
    /**
     * Gets the growth multiplier for the crop.
     * 
     * @return The crop's growth multiplier.
     */
    public float getGrowthMultiplier () {
        
        return this.growthMultiplier;
    }
    
    /**
     * Calculates the total world ticks for this crop to reach maturity if planted on a given
     * soil.
     * 
     * @param soil The soil to calculate growth time with.
     * @return The amount of world ticks it would take for this crop to reach maturity when
     *         planted on the given soil.
     */
    public int getGrowthTicksForSoil (SoilInfo soil) {
        
        return MathHelper.floor(soil.getTickRate() * this.getGrowthMultiplier() * this.growthTicks);
    }
    
    /**
     * Gets a random seed item. This is used when taking a seed out of a pot. Since seeds are
     * an ingredient multiple seeds may be possible. To ensure fairness this method will select
     * one of those items at random.
     * 
     * @return A random seed item.
     */
    public ItemStack getRandomSeed () {
        
        final ItemStack[] matchingStacks = this.seed.getMatchingStacks();
        return matchingStacks.length > 0 ? matchingStacks[Bookshelf.RANDOM.nextInt(matchingStacks.length)] : ItemStack.EMPTY;
    }
    
    public void setSeed (Ingredient seed) {
        
        this.seed = seed;
    }
    
    public void setSoilCategories (Set<String> soilCategories) {
        
        this.soilCategories = soilCategories;
    }
    
    public void setGrowthTicks (int growthTicks) {
        
        this.growthTicks = growthTicks;
    }
    
    public void setGrowthMultiplier (float growthMultiplier) {
        
        this.growthMultiplier = growthMultiplier;
    }
    
    public void setResults (List<HarvestEntry> results) {
        
        this.results = results;
    }
    
    public void setDisplayBlock (BlockState displayBlock) {
        
        this.displayBlock = displayBlock;
    }
}
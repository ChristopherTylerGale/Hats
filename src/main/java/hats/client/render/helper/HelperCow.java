package hats.client.render.helper;

import hats.api.RenderOnEntityHelper;
import hats.common.Hats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityCow;

public class HelperCow extends RenderOnEntityHelper {

	@Override
	public Class helperForClass() 
	{
		return EntityCow.class;
	}

    @Override
    public boolean canWearHat(EntityLivingBase living)
    {
        return Hats.config.getInt("hatCow") == 1;
    }

	@Override
	public float getRotatePointVert(EntityLivingBase ent)
	{
		return 20.2F/16F;
	}

	@Override
	public float getRotatePointHori(EntityLivingBase ent)
	{
		return 8F/16F;
	}

	@Override
	public float getOffsetPointVert(EntityLivingBase ent)
	{
		return 4F/16F;
	}

	@Override
	public float getOffsetPointHori(EntityLivingBase ent)
	{
		return 2F/16F;
	}

}

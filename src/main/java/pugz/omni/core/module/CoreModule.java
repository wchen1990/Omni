package pugz.omni.core.module;

import pugz.omni.common.tileentity.OmniBeehiveTileEntity;
import pugz.omni.common.tileentity.OmniChestTileEntity;
import pugz.omni.common.tileentity.OmniSignTileEntity;
import pugz.omni.common.tileentity.OmniTrappedChestTileEntity;
import pugz.omni.core.registry.OmniTileEntities;
import pugz.omni.core.util.RegistryUtil;

public class CoreModule extends AbstractModule {
    public static final CoreModule instance = new CoreModule();

    public CoreModule() {
        super("Core");
    }

    @Override
    protected void sendInitMessage() {
        System.out.println("Booted up the Core Module.");
    }

    @Override
    protected void onInitialize() {

    }

    @Override
    protected void registerTileEntities() {
        OmniTileEntities.BEEHIVE = RegistryUtil.createTileEntity("beehive", OmniBeehiveTileEntity::new, () -> OmniTileEntities.collectBlocks(OmniBeehiveTileEntity.class));
        OmniTileEntities.SIGN = RegistryUtil.createTileEntity("sign", OmniSignTileEntity::new, () -> OmniTileEntities.collectBlocks(OmniSignTileEntity.class));
        OmniTileEntities.CHEST = RegistryUtil.createTileEntity("chest", OmniChestTileEntity::new, () -> OmniTileEntities.collectBlocks(OmniChestTileEntity.class));
        OmniTileEntities.TRAPPED_CHEST = RegistryUtil.createTileEntity("trapped_chest", OmniTrappedChestTileEntity::new, () -> OmniTileEntities.collectBlocks(OmniTrappedChestTileEntity.class));
    }
}
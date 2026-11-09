package ovh.pdx.qmine.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import ovh.pdx.qmine.client.gui.QMineConfigScreen;
import ovh.pdx.qmine.client.gui.QMineYaclScreen;

public class QMineModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                return QMineYaclScreen.create(parent);
            } catch (Throwable t) {
                return new QMineConfigScreen(parent);
            }
        };
    }
}

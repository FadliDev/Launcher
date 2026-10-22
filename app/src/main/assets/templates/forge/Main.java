package {{package}};

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod({{className}}.MOD_ID)
public class {{className}} {
    public static final String MOD_ID = "{{modId}}";
    private static final Logger LOGGER = LogUtils.getLogger();

    public {{className}}() {
        LOGGER.info("{{modName}} loaded");
    }
}

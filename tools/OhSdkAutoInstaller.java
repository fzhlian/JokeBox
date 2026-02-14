import com.huawei.deveco.sdkmanager.core.api.PathAndApiVersion;
import com.huawei.deveco.sdkmanager.core.api.SdkInfoHandler;
import com.huawei.deveco.sdkmanager.core.api.SdkInfoProgress;
import com.huawei.deveco.sdkmanager.core.domain.Component;
import com.huawei.deveco.sdkmanager.core.domain.License;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkConfig;
import com.huawei.deveco.sdkmanager.ohos.common.api.OhPrjSdkType;
import com.huawei.deveco.sdkmanager.ohos.common.api.SimpleOhPrjSdkHandler;
import com.huawei.deveco.sdkmanager.ohos.core.license.OhLicenseManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class OhSdkAutoInstaller {
    public static void main(String[] args) {
        try {
            String sdkRoot = args.length > 0 ? args[0] : "C:/Users/fzhlian/AppData/Local/Huawei/Sdk";
            String nodeHome = args.length > 1 ? args[1] : "d:/fzhlian/tools/node18/node-v18.20.8-win-x64";

            OhPrjSdkConfig cfg = new OhPrjSdkConfig.Builder(sdkRoot)
                    .nodeHome(nodeHome)
                    .build();
            SdkInfoHandler handler = new SimpleOhPrjSdkHandler(cfg).getSdkHandler(OhPrjSdkType.OPENHARMONY.value());

            Set<Integer> apis = handler.getApiVersions();
            if (apis == null || apis.isEmpty()) {
                System.out.println("No API versions returned from sdk manager.");
                System.exit(2);
            }

            int targetApi = apis.stream().max(Integer::compareTo).orElse(9);
            System.out.println("Detected API versions: " + apis + ", target=" + targetApi);

            String[] components = new String[] {"toolchains", "ets", "js", "native", "previewer"};
            Set<PathAndApiVersion> requests = new LinkedHashSet<>();
            for (String c : components) {
                requests.add(new PathAndApiVersion(c, targetApi));
            }

            Map<PathAndApiVersion, Component> result;
            try {
                result = handler.getOrDownload(requests);
            } catch (Throwable firstError) {
                System.out.println("First install attempt failed, trying auto-accept licenses...");
                autoAcceptLicenses(handler);
                result = handler.getOrDownload(requests);
            }

            if (result == null || result.isEmpty()) {
                System.out.println("Install returned empty result.");
                System.exit(3);
            }

            System.out.println("Installed/resolved components:");
            for (Map.Entry<PathAndApiVersion, Component> e : result.entrySet()) {
                Component c = e.getValue();
                System.out.println("- " + e.getKey().getPath() + "@" + e.getKey().getApiVersion() +
                        " => " + (c == null ? "null" : c.getDisplayName() + " | " + c.getLocation()));
            }

            Map<PathAndApiVersion, Component> local = handler.getLocalSdks();
            System.out.println("Local SDK count=" + (local == null ? 0 : local.size()));
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void autoAcceptLicenses(SdkInfoHandler handler) throws Exception {
        SdkInfoProgress progress = new SdkInfoProgress();

        Method getRemoteComponents = handler.getClass().getDeclaredMethod("getRemoteComponents", com.huawei.deveco.sdkmanager.core.domain.Progress.class);
        getRemoteComponents.setAccessible(true);
        @SuppressWarnings("unchecked")
        Collection<Component> remote = (Collection<Component>) getRemoteComponents.invoke(handler, progress);

        Set<String> licenseIds = remote.stream()
                .map(Component::getLicense)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (licenseIds.isEmpty()) {
            System.out.println("No remote license IDs found.");
            return;
        }

        Field netClientField = handler.getClass().getDeclaredField("netClient");
        netClientField.setAccessible(true);
        Object netClient = netClientField.get(handler);

        Field settingsField = handler.getClass().getSuperclass().getDeclaredField("settings");
        settingsField.setAccessible(true);
        Object settings = settingsField.get(handler);

        OhLicenseManager licenseManager = new OhLicenseManager(
                (com.huawei.deveco.sdkmanager.core.net.NetClient) netClient,
                (com.huawei.deveco.sdkmanager.core.domain.SdkSettings) settings
        );

        Collection<License> unaccepted = licenseManager.findUnacceptedLicenses(licenseIds, progress);
        if (unaccepted == null || unaccepted.isEmpty()) {
            System.out.println("No unaccepted licenses.");
            return;
        }

        System.out.println("Accepting licenses count=" + unaccepted.size());
        licenseManager.acceptLicenses(unaccepted, progress);
        System.out.println("License acceptance done.");
    }
}


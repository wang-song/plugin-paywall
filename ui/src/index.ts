import { definePlugin } from "@halo-dev/console-shared";
import HomeView from "./views/HomeView.vue";
import { IconPlug } from "@halo-dev/components";
import { markRaw } from "vue";
import { extensionPaywall } from "./paywall";

interface Editor {
  chain: () => {
    focus: () => {
      setNode: (type: string, attrs: Record<string, any>) => {
        run: () => void;
      };
    };
  };
  getSelectedText: () => string;
}

export default definePlugin({
  components: {
  },
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/vmq",
        name: "VMQ",
        component: HomeView,
        meta: {
          title: "V免签配置",
          searchable: true,
          menu: {
            name: "V免签配置",
            group: "支付",
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {
    "default:editor:extension:create": () => {
      return [
        markRaw(extensionPaywall),
      ];
    },
  },
});

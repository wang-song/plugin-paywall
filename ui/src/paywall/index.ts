import {
  Node,
  VueNodeViewRenderer,
  mergeAttributes,
  ToolboxItem,
} from "@halo-dev/richtext-editor";
import type { Editor } from "@halo-dev/richtext-editor";
import { markRaw } from "vue";
import MdiCurrencyUsd from "~icons/mdi/currency-usd";
import PaywallComponent from "./PaywallComponent.vue";

interface PaywallAttributes {
  price: string;
  preview: string;
}

interface HaloExtension {
  name: string;
  type: string;
  addOptions?: () => any;
  addCommands?: () => any;
  button?: {
    component: any;
    props: Record<string, any>;
  };
}

/**
 * 创建付费内容扩展
 * 用于在富文本编辑器中实现付费内容的功能
 */
export const extensionPaywall = Node.create({
  // 节点基本配置
  name: "paywall",
  group: "block",
  content: "block+",
  atom: false,
  inline: false,
  selectable: true,
  draggable: true,
  isolating: true,
  allowGapCursor: true,

  /**
   * 添加节点属性
   * 定义节点可以具有的属性及其处理方式
   */
  addAttributes() {
    return {
      price: {
        default: '',
        parseHTML: element => element.getAttribute('data-price'),
        renderHTML: attributes => {
          if (!attributes.price) {
            return {};
          }
          return {
            'data-price': attributes.price
          };
        }
      },
      preview: {
        default: '',
        parseHTML: element => element.getAttribute('data-preview'),
        renderHTML: attributes => {
          if (!attributes.preview) {
            return {};
          }
          return {
            'data-preview': attributes.preview
          };
        }
      }
    }
  },

  /**
   * 定义如何从HTML解析节点
   * 指定哪些HTML标签和属性会被识别为付费节点
   */
  parseHTML() {
    return [
      {
        tag: 'div[data-type="paywall"]'
      }
    ]
  },

  /**
   * 定义如何将节点渲染为HTML
   * @param HTMLAttributes 节点的HTML属性
   */
  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes({ 'data-type': 'paywall' }, HTMLAttributes), 0]
  },

  /**
   * 添加节点视图
   * 使用Vue组件渲染节点的可视化界面
   */
  addNodeView() {
    return VueNodeViewRenderer(PaywallComponent)
  },

  /**
   * 添加扩展选项
   * 配置工具箱项目，用于在编辑器中插入付费内容
   */
  addOptions() {
    return {
      ...this.parent?.(),
      getToolboxItems({ editor }: { editor: Editor }) {
        return [
          {
            priority: 99,
            component: markRaw(ToolboxItem),
            props: {
              editor,
              icon: markRaw(MdiCurrencyUsd),
              title: "插入付费内容",
              action: () => {
                editor
                  .chain()
                  .focus()
                  .insertContent({
                    type: "paywall",
                    attrs: {
                      price: "1",
                      preview: ""
                    },
                    content: [{
                      type: 'paragraph',
                      content: [{ 
                        type: 'text', 
                        text: '在此输入需要付费的内容' 
                      }]
                    }]
                  })
                  .run();
              },
            },
          },
        ];
      },
    };
  },
}); 
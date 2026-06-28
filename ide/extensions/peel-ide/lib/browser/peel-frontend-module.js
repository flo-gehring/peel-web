"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const inversify_1 = require("@theia/core/shared/inversify");
const browser_1 = require("@theia/core/lib/browser");
const common_1 = require("@theia/core/lib/common");
const peel_command_contribution_1 = require("./peel-command-contribution");
const peel_frontend_contribution_1 = require("./peel-frontend-contribution");
const peel_keybinding_contribution_1 = require("./peel-keybinding-contribution");
const peel_menu_contribution_1 = require("./peel-menu-contribution");
const peel_view_contribution_1 = require("./peel-view-contribution");
const peel_frontend_service_1 = require("./services/peel-frontend-service");
const peel_project_tree_model_1 = require("./widgets/peel-project-tree-model");
const peel_project_tree_widget_1 = require("./widgets/peel-project-tree-widget");
exports.default = new inversify_1.ContainerModule((bind) => {
    bind(peel_frontend_service_1.PeelFrontendService).toSelf().inSingletonScope();
    bind(common_1.CommandContribution).to(peel_command_contribution_1.PeelCommandContribution).inSingletonScope();
    bind(common_1.MenuContribution).to(peel_menu_contribution_1.PeelMenuContribution).inSingletonScope();
    bind(browser_1.KeybindingContribution).to(peel_keybinding_contribution_1.PeelKeybindingContribution).inSingletonScope();
    bind(browser_1.FrontendApplicationContribution).to(peel_frontend_contribution_1.PeelFrontendContribution).inSingletonScope();
    (0, browser_1.bindViewContribution)(bind, peel_view_contribution_1.PeelViewContribution);
    bind(browser_1.FrontendApplicationContribution).toService(peel_view_contribution_1.PeelViewContribution);
    bind(browser_1.WidgetFactory)
        .toDynamicValue((ctx) => ({
        id: peel_project_tree_widget_1.PeelProjectTreeWidget.ID,
        createWidget: () => {
            const child = (0, browser_1.createTreeContainer)(ctx.container, {
                model: peel_project_tree_model_1.PeelProjectTreeModel,
                widget: peel_project_tree_widget_1.PeelProjectTreeWidget,
                props: {
                    search: true,
                    multiSelect: false,
                },
            });
            child.bind(peel_project_tree_model_1.PeelProjectTreeModel).toSelf().inSingletonScope();
            child.bind(browser_1.TreeModel).toService(peel_project_tree_model_1.PeelProjectTreeModel);
            child.bind(peel_project_tree_widget_1.PeelProjectTreeWidget).toSelf();
            return child.get(peel_project_tree_widget_1.PeelProjectTreeWidget);
        },
    }))
        .inSingletonScope();
});
//# sourceMappingURL=peel-frontend-module.js.map
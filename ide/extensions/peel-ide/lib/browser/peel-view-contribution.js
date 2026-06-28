"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.PeelViewContribution = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const browser_1 = require("@theia/core/lib/browser");
const peel_commands_1 = require("./peel-commands");
const peel_project_tree_widget_1 = require("./widgets/peel-project-tree-widget");
let PeelViewContribution = class PeelViewContribution extends browser_1.AbstractViewContribution {
    constructor() {
        super({
            widgetId: peel_project_tree_widget_1.PeelProjectTreeWidget.ID,
            widgetName: peel_project_tree_widget_1.PeelProjectTreeWidget.LABEL,
            defaultWidgetOptions: {
                area: 'left',
            },
            toggleCommandId: peel_commands_1.PeelCommands.OpenProjectsView.id,
            toggleKeybinding: 'ctrlcmd+alt+p',
        });
    }
    async onStart(_app) {
        await this.openView({ activate: false, reveal: true });
    }
};
exports.PeelViewContribution = PeelViewContribution;
exports.PeelViewContribution = PeelViewContribution = __decorate([
    (0, inversify_1.injectable)(),
    __metadata("design:paramtypes", [])
], PeelViewContribution);
//# sourceMappingURL=peel-view-contribution.js.map
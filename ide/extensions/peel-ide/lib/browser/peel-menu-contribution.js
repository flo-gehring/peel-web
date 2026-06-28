"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.PeelMenuContribution = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const browser_1 = require("@theia/core/lib/browser");
const peel_commands_1 = require("./peel-commands");
let PeelMenuContribution = class PeelMenuContribution {
    registerMenus(menus) {
        menus.registerMenuAction(browser_1.CommonMenus.FILE_NEW, {
            commandId: peel_commands_1.PeelCommands.NewProject.id,
            label: 'PEEL Project',
        });
        menus.registerMenuAction(browser_1.CommonMenus.VIEW_VIEWS, {
            commandId: peel_commands_1.PeelCommands.OpenProjectsView.id,
            label: 'Peel Projects',
        });
        menus.registerMenuAction(browser_1.CommonMenus.VIEW_VIEWS, {
            commandId: peel_commands_1.PeelCommands.RefreshProjects.id,
            label: 'Refresh Peel Projects',
        });
    }
};
exports.PeelMenuContribution = PeelMenuContribution;
exports.PeelMenuContribution = PeelMenuContribution = __decorate([
    (0, inversify_1.injectable)()
], PeelMenuContribution);
//# sourceMappingURL=peel-menu-contribution.js.map
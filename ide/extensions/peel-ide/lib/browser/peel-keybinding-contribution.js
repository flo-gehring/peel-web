"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.PeelKeybindingContribution = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const peel_commands_1 = require("./peel-commands");
let PeelKeybindingContribution = class PeelKeybindingContribution {
    registerKeybindings(keybindings) {
        keybindings.registerKeybinding({
            command: peel_commands_1.PeelCommands.RunDefault.id,
            keybinding: 'ctrlcmd+enter',
        });
        keybindings.registerKeybinding({
            command: peel_commands_1.PeelCommands.ValidateDefault.id,
            keybinding: 'shift+ctrlcmd+enter',
        });
        keybindings.registerKeybinding({
            command: peel_commands_1.PeelCommands.OpenProjectsView.id,
            keybinding: 'ctrlcmd+alt+p',
        });
    }
};
exports.PeelKeybindingContribution = PeelKeybindingContribution;
exports.PeelKeybindingContribution = PeelKeybindingContribution = __decorate([
    (0, inversify_1.injectable)()
], PeelKeybindingContribution);
//# sourceMappingURL=peel-keybinding-contribution.js.map
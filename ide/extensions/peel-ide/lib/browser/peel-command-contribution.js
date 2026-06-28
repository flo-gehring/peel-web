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
exports.PeelCommandContribution = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const common_1 = require("@theia/core/lib/common");
const peel_commands_1 = require("./peel-commands");
const peel_project_tree_model_1 = require("./widgets/peel-project-tree-model");
let PeelCommandContribution = class PeelCommandContribution {
    messageService;
    treeModel;
    registerCommands(registry) {
        registry.registerCommand(peel_commands_1.PeelCommands.NewProject, {
            execute: () => this.messageService.info('Create project flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.NewScript, {
            execute: () => this.messageService.info('Create script flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.NewDocument, {
            execute: () => this.messageService.info('Create document flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.NewRenderConfig, {
            execute: () => this.messageService.info('Create render config flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.RunDefault, {
            execute: () => this.messageService.info('Run default flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.ValidateDefault, {
            execute: () => this.messageService.info('Validate default flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.RenderPreview, {
            execute: () => this.messageService.info('Render preview flow will be added next.'),
        });
        registry.registerCommand(peel_commands_1.PeelCommands.OpenProjectsView, {
            execute: () => {
                this.messageService.info('Peel projects view is available in the left panel.');
            },
        });
        registry.registerCommand(peel_commands_1.PeelCommands.RefreshProjects, {
            execute: async () => {
                await this.treeModel.refreshTree();
                this.messageService.info('Projects refreshed.');
            },
        });
    }
};
exports.PeelCommandContribution = PeelCommandContribution;
__decorate([
    (0, inversify_1.inject)(common_1.MessageService),
    __metadata("design:type", common_1.MessageService)
], PeelCommandContribution.prototype, "messageService", void 0);
__decorate([
    (0, inversify_1.inject)(peel_project_tree_model_1.PeelProjectTreeModel),
    __metadata("design:type", peel_project_tree_model_1.PeelProjectTreeModel)
], PeelCommandContribution.prototype, "treeModel", void 0);
exports.PeelCommandContribution = PeelCommandContribution = __decorate([
    (0, inversify_1.injectable)()
], PeelCommandContribution);
//# sourceMappingURL=peel-command-contribution.js.map
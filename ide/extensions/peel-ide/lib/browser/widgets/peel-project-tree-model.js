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
exports.PeelProjectTreeModel = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const browser_1 = require("@theia/core/lib/browser");
const peel_frontend_service_1 = require("../services/peel-frontend-service");
const ROOT_ID = 'peel-projects-root';
let PeelProjectTreeModel = class PeelProjectTreeModel extends browser_1.TreeModelImpl {
    peelFrontendService;
    init() {
        super.init();
        void this.refreshTree();
    }
    async refreshTree() {
        const projects = await this.peelFrontendService.listProjects();
        const root = {
            id: ROOT_ID,
            name: 'PEEL Projects',
            parent: undefined,
            visible: false,
            children: [],
            kind: 'root',
        };
        for (const project of projects) {
            const projectNode = {
                id: `peel-project-${project.id}`,
                name: project.name,
                parent: root,
                children: [],
                kind: 'project',
                projectId: project.id,
                description: project.description ?? undefined,
                expanded: true,
            };
            browser_1.CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'scripts', 'Scripts'));
            browser_1.CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'documents', 'Documents'));
            browser_1.CompositeTreeNode.addChild(projectNode, this.newGroupNode(projectNode, 'render-configs', 'Render Configurations'));
            browser_1.CompositeTreeNode.addChild(root, projectNode);
        }
        this.tree.root = root;
    }
    newGroupNode(parent, groupType, name) {
        return {
            id: `${parent.id}-${groupType}`,
            name,
            parent,
            children: [],
            kind: 'group',
            groupType,
            expanded: false,
        };
    }
};
exports.PeelProjectTreeModel = PeelProjectTreeModel;
__decorate([
    (0, inversify_1.inject)(peel_frontend_service_1.PeelFrontendService),
    __metadata("design:type", peel_frontend_service_1.PeelFrontendService)
], PeelProjectTreeModel.prototype, "peelFrontendService", void 0);
__decorate([
    (0, inversify_1.postConstruct)(),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], PeelProjectTreeModel.prototype, "init", null);
exports.PeelProjectTreeModel = PeelProjectTreeModel = __decorate([
    (0, inversify_1.injectable)()
], PeelProjectTreeModel);
//# sourceMappingURL=peel-project-tree-model.js.map
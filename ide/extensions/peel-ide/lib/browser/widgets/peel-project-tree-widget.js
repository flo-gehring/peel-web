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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var PeelProjectTreeWidget_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.PeelProjectTreeWidget = void 0;
const jsx_runtime_1 = require("react/jsx-runtime");
const inversify_1 = require("@theia/core/shared/inversify");
const browser_1 = require("@theia/core/lib/browser");
const peel_project_tree_model_1 = require("./peel-project-tree-model");
const peel_project_node_1 = require("./peel-project-node");
let PeelProjectTreeWidget = class PeelProjectTreeWidget extends browser_1.TreeWidget {
    static { PeelProjectTreeWidget_1 = this; }
    static ID = 'peel-projects-tree';
    static LABEL = 'Peel Projects';
    constructor(props, model, contextMenuRenderer) {
        super(props, model, contextMenuRenderer);
        this.id = PeelProjectTreeWidget_1.ID;
        this.title.label = PeelProjectTreeWidget_1.LABEL;
        this.title.caption = PeelProjectTreeWidget_1.LABEL;
        this.title.closable = true;
        this.title.iconClass = 'codicon codicon-folder-library';
    }
    renderIcon(node, props) {
        if (peel_project_node_1.PeelRootNode.is(node)) {
            return (0, jsx_runtime_1.jsx)("div", { className: "codicon codicon-folder-opened" });
        }
        if (peel_project_node_1.PeelProjectNode.is(node)) {
            return (0, jsx_runtime_1.jsx)("div", { className: "codicon codicon-package" });
        }
        if (peel_project_node_1.PeelTreeGroupNode.is(node)) {
            return (0, jsx_runtime_1.jsx)("div", { className: "codicon codicon-folder" });
        }
        return super.renderIcon(node, props);
    }
    toNodeName(node) {
        if (peel_project_node_1.PeelRootNode.is(node) || peel_project_node_1.PeelProjectNode.is(node) || peel_project_node_1.PeelTreeGroupNode.is(node)) {
            return node.name ?? node.id;
        }
        return super.toNodeName(node);
    }
};
exports.PeelProjectTreeWidget = PeelProjectTreeWidget;
exports.PeelProjectTreeWidget = PeelProjectTreeWidget = PeelProjectTreeWidget_1 = __decorate([
    (0, inversify_1.injectable)(),
    __param(0, (0, inversify_1.inject)(browser_1.TreeProps)),
    __param(1, (0, inversify_1.inject)(peel_project_tree_model_1.PeelProjectTreeModel)),
    __param(2, (0, inversify_1.inject)(browser_1.ContextMenuRenderer)),
    __metadata("design:paramtypes", [Object, peel_project_tree_model_1.PeelProjectTreeModel,
        browser_1.ContextMenuRenderer])
], PeelProjectTreeWidget);
//# sourceMappingURL=peel-project-tree-widget.js.map
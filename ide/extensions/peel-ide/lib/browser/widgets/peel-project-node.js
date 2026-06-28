"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PeelRootNode = exports.PeelProjectNode = exports.PeelTreeGroupNode = void 0;
exports.isPeelNode = isPeelNode;
const browser_1 = require("@theia/core/lib/browser");
var PeelTreeGroupNode;
(function (PeelTreeGroupNode) {
    function is(node) {
        return browser_1.CompositeTreeNode.is(node) && browser_1.ExpandableTreeNode.is(node) && node.kind === 'group';
    }
    PeelTreeGroupNode.is = is;
})(PeelTreeGroupNode || (exports.PeelTreeGroupNode = PeelTreeGroupNode = {}));
var PeelProjectNode;
(function (PeelProjectNode) {
    function is(node) {
        return browser_1.CompositeTreeNode.is(node) && browser_1.ExpandableTreeNode.is(node) && node.kind === 'project';
    }
    PeelProjectNode.is = is;
})(PeelProjectNode || (exports.PeelProjectNode = PeelProjectNode = {}));
var PeelRootNode;
(function (PeelRootNode) {
    function is(node) {
        return browser_1.CompositeTreeNode.is(node) && node.kind === 'root';
    }
    PeelRootNode.is = is;
})(PeelRootNode || (exports.PeelRootNode = PeelRootNode = {}));
function isPeelNode(node) {
    return PeelRootNode.is(node) || PeelProjectNode.is(node) || PeelTreeGroupNode.is(node);
}
//# sourceMappingURL=peel-project-node.js.map
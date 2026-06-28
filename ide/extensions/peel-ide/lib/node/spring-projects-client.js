"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SpringProjectsClient = void 0;
const inversify_1 = require("@theia/core/shared/inversify");
const DEFAULT_API_BASE_URL = 'http://localhost:8080';
let SpringProjectsClient = class SpringProjectsClient {
    async listProjects() {
        const response = await fetch(`${this.apiBaseUrl()}/api/projects`);
        if (!response.ok) {
            throw new Error(`Failed to load projects: ${response.status} ${response.statusText}`);
        }
        const payload = (await response.json());
        if (!Array.isArray(payload.projects)) {
            throw new Error('Invalid projects payload received from backend.');
        }
        return payload.projects;
    }
    apiBaseUrl() {
        const configured = process.env.PEEL_API_BASE_URL?.trim();
        if (!configured) {
            return DEFAULT_API_BASE_URL;
        }
        return configured.endsWith('/') ? configured.slice(0, -1) : configured;
    }
};
exports.SpringProjectsClient = SpringProjectsClient;
exports.SpringProjectsClient = SpringProjectsClient = __decorate([
    (0, inversify_1.injectable)()
], SpringProjectsClient);
//# sourceMappingURL=spring-projects-client.js.map
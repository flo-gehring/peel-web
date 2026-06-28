"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const inversify_1 = require("@theia/core/shared/inversify");
const common_1 = require("@theia/core/lib/common");
const peel_protocol_1 = require("../common/peel-protocol");
const peel_backend_service_1 = require("./peel-backend-service");
const spring_projects_client_1 = require("./spring-projects-client");
exports.default = new inversify_1.ContainerModule((bind) => {
    bind(spring_projects_client_1.SpringProjectsClient).toSelf().inSingletonScope();
    bind(peel_backend_service_1.PeelBackendService).toSelf().inSingletonScope();
    bind(peel_protocol_1.PeelService).toService(peel_backend_service_1.PeelBackendService);
    bind(common_1.ConnectionHandler)
        .toDynamicValue((ctx) => new common_1.RpcConnectionHandler(peel_protocol_1.PeelServicePath, () => ctx.container.get(peel_backend_service_1.PeelBackendService)))
        .inSingletonScope();
});
//# sourceMappingURL=peel-backend-module.js.map
/**
 * This file can be edited to adjust the ESBuild build process.
 * To reset, delete this file and rerun theia build again.
 */
import { browserOptions, watch } from './gen-esbuild.browser.mjs';
import { nodeOptions } from './gen-esbuild.node.mjs';

import esbuild from 'esbuild';
import path from 'node:path';
import resolvePackagePath from 'resolve-package-path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const monacoEditorCorePackageJson = resolvePackagePath('@theia/monaco-editor-core', __dirname);
const codiconTtfFallbackPath = monacoEditorCorePackageJson
    ? path.join(
        path.dirname(monacoEditorCorePackageJson),
        'esm',
        'vs',
        'base',
        'browser',
        'ui',
        'codicons',
        'codicon',
        'codicon.ttf',
    )
    : undefined;

const codiconTtfFallbackPlugin = {
    name: 'codicon-ttf-fallback-plugin',
    setup(build) {
        build.onResolve({ filter: /codicon\.ttf(\?.*)?$/ }, args => {
            if (!codiconTtfFallbackPath || !args.path.startsWith('./codicon.ttf')) {
                return undefined;
            }
            return { path: codiconTtfFallbackPath };
        });
    }
};

const patchedBrowserOptions = {
    ...browserOptions,
    plugins: [
        codiconTtfFallbackPlugin,
        ...browserOptions.plugins,
    ]
};

const browserContext = await esbuild.context(patchedBrowserOptions);
const nodeContext = await esbuild.context(nodeOptions);


if (watch) {
    await Promise.all([
        browserContext.watch(),
        nodeContext.watch(),
    ]);
} else {
    try {
        await browserContext.rebuild();
        await browserContext.dispose();
        await nodeContext.rebuild();
        await nodeContext.dispose();
    } catch {
        process.exit(1);
    }
}

<!-- GroupActions.vue -->
<script setup lang="ts">
import type { IDockviewHeaderActionsProps } from 'dockview-vue';
import { computed } from 'vue';

// Dockview passes a `params` prop to header action components
const props = defineProps<{
    params: IDockviewHeaderActionsProps;
}>();

const showActions = computed(() => props.params.activePanel?.id.startsWith('editor-') ?? false);

// Perform actions using the group API or container API
const handleAddPanel = () => {
    props.params.containerApi.addPanel({
        id: `editor-Untitled-${Date.now()}`,
        component: 'editor',
        title: 'Untitled',
        position: {
            referenceGroup: props.params.group.id, // Adds panel to this specific group
        },
    });
};

const handleCustomAction = () => {
    console.log('Active Panel in Group:', props.params.activePanel?.id);
    console.log('Group ID:', props.params.group.id);
};
</script>

<template>
    <div v-if="showActions" class="group-header-actions">
        <button class="action-btn" title="Add Panel to Group" @click="handleAddPanel">
            ➕
        </button>
        <button class="action-btn" title="Run Action" @click="handleCustomAction">
            ⚙️
        </button>
    </div>
</template>

<style scoped>
.group-header-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 0 4px;
}

.action-btn {
    background: transparent;
    border: none;
    cursor: pointer;
    color: inherit;
    font-size: 12px;
    padding: 2px 4px;
    border-radius: 3px;
}

.action-btn:hover {
    background-color: rgba(255, 255, 255, 0.1);
}
</style>
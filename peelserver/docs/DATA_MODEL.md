# Data Modell

````mermaid
classDiagram
    class ScriptId {
        String id
    }

    class DocumentId {
        String id
    }

    class PeelScriptPersistence {
        ScriptId id
        PeelScriptData data
    }

    class PeelScriptData {
        String description
        String script
    }
    
    PeelScriptPersistence "1" *--> "1" PeelScriptData
    class DocumentPersistence {
        DocumentId id
        DocumentData  documentData
    }
        
    class DocumentPersistenceData {
        Map[String, PeelScriptId] scriptTags
        String documentContent,
        ExpressionRenderConfigurationId expressionRenderConfigurationId,
        ExpressionRenderConfigurationData localOverrides
    }
    
    DocumentPersistenceData "0..n" o--> "1" RenderConfigurationPersistence
    DocumentPersistenceData "0..n" o--> "1" PeelScriptPersistence
    
    class Document {
        Map[String, PeelScript] scriptTags
        String documentContent,
        ExpressionRenderConfigurationData config,
        ExpressionRenderConfigurationData localOverrides
    }
    
    DocumentPersistenceData -->  Document: transforms to
    DocumentPersistence "1" *--> "1" DocumentPersistenceData
    DocumentPersistence "1" *--> "1" DocumentId

    class RenderConfigurationPersistence {
        ExpressionRenderConfigId Id
        ExpressionRenderConfigurationData data
    }
    class ExpressionRenderConfiguration {
        Map[TraceExpressionKind, renderTemplate] renderTemplates
    }

    RenderConfigurationPersistence "1" *--> "1" ExpressionRenderConfiguration
    RenderConfigurationPersistence "1" *--> "1" ExpressionRenderConfigurationId
    ExpressionRenderConfiguration "0..n" *--> "0..n" TraceExpressionKind
    Document "0..*" *--> "0..*" PeelScript
    Document "1" *--> "2" ExpressionRenderConfiguration
    PeelScript "1" *--> "1" ScriptId

    class TraceExpressionKind {
        <<enumeration>>
        LITERAL,
        BINARY_OPERATOR,
        UNARY_PREFIX_OPERATOR,
        VARIABLE_NAME,
        FUNCTION_CALL,
        RETURN_EXPR,
        ASSIGNMENT,
        IF_STATEMENT,
        WHILE_LOOP,
        FOR_EACH_LOOP,
        LIST_LITERAL,
        MAP_LITERAL,
        SELECTOR,
        BLOCK
    }
````
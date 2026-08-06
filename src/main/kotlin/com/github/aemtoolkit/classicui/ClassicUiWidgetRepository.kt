package com.github.aemtoolkit.classicui

/** Independently curated metadata for common AEM Classic UI dialog widgets. */
object ClassicUiWidgetRepository {
    private val common = listOf(
        field("name", "JCR property path written by the field."),
        field("fieldLabel", "Label displayed beside the field."),
        field("fieldDescription", "Help text displayed for the field."),
        field("defaultValue", "Value used when no stored value is available."),
        field("disabled", "Disables editing when true."),
        field("hidden", "Hides the widget when true."),
        field("width", "Widget width in pixels."),
    )

    private fun widget(
        xtype: String,
        description: String,
        vararg fields: ClassicUiField,
    ) = ClassicUiWidget(xtype, description, (common + fields).distinctBy { it.name })

    private fun field(name: String, description: String) =
        ClassicUiField(name, description)

    private val widgets = listOf(
        widget("textfield", "Single-line text input.",
            field("allowBlank", "Allows an empty value when true."),
            field("maxLength", "Maximum number of accepted characters."),
            field("regex", "Regular expression used to validate the value.")),
        widget("textarea", "Multi-line text input.",
            field("grow", "Expands the field as content grows."),
            field("height", "Widget height in pixels.")),
        widget("richtext", "Rich text editor.",
            field("externalStyleSheets", "Style sheets loaded by the editor.")),
        widget("selection", "Select, radio, or checkbox option field.",
            field("type", "Selection presentation: select, radio, or checkbox."),
            field("options", "Static or remote options configuration."),
            field("optionsProvider", "Provider used to load dynamic options.")),
        widget("checkbox", "Boolean checkbox field.",
            field("inputValue", "Value stored when selected."),
            field("checked", "Initial selected state.")),
        widget("radio", "Single radio option.",
            field("inputValue", "Value stored when selected.")),
        widget("radiogroup", "Group of related radio options.",
            field("columns", "Number or layout of option columns.")),
        widget("pathfield", "Repository path picker.",
            field("rootPath", "Root path available in the picker."),
            field("predicate", "Repository predicate used to filter results.")),
        widget("multifield", "Repeating field that stores multiple values.",
            field("orderable", "Allows authors to reorder entries.")),
        widget("compositefield", "Groups multiple widgets into one row."),
        widget("numberfield", "Numeric input field.",
            field("allowDecimals", "Allows decimal values when true."),
            field("minValue", "Minimum accepted value."),
            field("maxValue", "Maximum accepted value.")),
        widget("spinner", "Numeric input with increment and decrement controls.",
            field("incrementValue", "Amount added or removed per step.")),
        widget("datefield", "Date input and date picker.",
            field("format", "Display and parsing format."),
            field("minValue", "Earliest accepted date."),
            field("maxValue", "Latest accepted date.")),
        widget("password", "Masked single-line text input."),
        widget("hidden", "Hidden value submitted with the dialog."),
        widget("fileupload", "File upload field.",
            field("fileNameParameter", "Request parameter used for the file name."),
            field("fileReferenceParameter", "Property used for an existing asset reference.")),
        widget("html5smartfile", "Asset upload and selection field.",
            field("mimeTypes", "Accepted MIME types."),
            field("cropParameter", "Property used to store crop coordinates.")),
        widget("panel", "Container for child widgets.",
            field("title", "Panel title.")),
        widget("tabpanel", "Container that displays child panels as tabs.",
            field("activeTab", "Initially selected tab.")),
        widget("dialogfieldset", "Titled group of related dialog fields.",
            field("title", "Fieldset title."),
            field("collapsible", "Allows the fieldset to collapse.")),
        widget("widgetcollection", "Collection of child widget definitions."),
        widget("static", "Read-only text displayed in a dialog.",
            field("text", "Text displayed by the widget.")),
        widget("label", "Read-only field label.",
            field("text", "Label text.")),
        widget("button", "Clickable dialog button.",
            field("text", "Button label."),
            field("handler", "Client-side click handler.")),
    ).associateBy(ClassicUiWidget::xtype)

    fun all(): List<ClassicUiWidget> = widgets.values.sortedBy(ClassicUiWidget::xtype)

    fun find(xtype: String): ClassicUiWidget? = widgets[xtype]
}

/** Classic UI xtype and its supported XML attributes. */
data class ClassicUiWidget(
    val xtype: String,
    val description: String,
    val fields: List<ClassicUiField>,
)

/** One XML attribute supported by a Classic UI widget. */
data class ClassicUiField(
    val name: String,
    val description: String,
)

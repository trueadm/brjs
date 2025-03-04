'use strict';

var MapUtility = require('br/util/MapUtility');
var PresentationNode = require('br/presenter/node/PresentationNode');
var Core = require('br/Core');
var FieldValuePropertyListener = require('br/presenter/node/FieldValuePropertyListener');
var ValidSelectionValidator = require('br/presenter/validator/ValidSelectionValidator');
var Errors = require('br/Errors');
var EditableProperty = require('br/presenter/property/EditableProperty');
var Property = require('br/presenter/property/Property');
var OptionsNodeList = require('br/presenter/node/OptionsNodeList');
var WritableProperty = require('br/presenter/property/WritableProperty');

/**
 * @module br/presenter/node/SelectionField
 */

/**
 * Constructs a new instance of <code>SelectionField</code>.
 * 
 * @class
 * @alias module:br/presenter/node/SelectionField
 * @extends module:br/presenter/node/PresentationNode
 * 
 * @classdesc
 * A <code>PresentationNode</code> containing all of the attributes necessary to
 * model a selection field on screen.
 * 
 * <p>Selection fields can be rendered using a number of different controls:</p>
 * 
 * <ul>
 *   <li>radio buttons</li>
 *   <li>select box</li>
 *   <li>combo box</li>
 *   <li>toggle switch (only when there are exactly two options)</li>
 * </ul>
 * 
 * <p>By default, selection fields automatically add a validator for the case where the
 * currently entered {@link #value} isn't one of the available {@link #options} &mdash; this can
 * occur when you render the selection field using a combo box, or the underlying options
 * change so the currently selected value is no longer an available option. You can disable these
 * validation errors by invoking {@link #allowInvalidSelections} with <code>true</code>.</p>
 * 
 * @param {Object} vOptions The list of available options, either using an array, a map or an {@link module:br/presenter/node/OptionsNodeList}.
 * @param {Object} vValue (optional) The initial value of the field, either using a primitive type or an {@link module:br/presenter/property/EditableProperty}.
 */
function SelectionField(vOptions, vValue) {
	/** @private */
	this.m_bAutomaticallyUpdateValueWhenOptionsChange = false;

	/**
	 * The textual label associated with the selection field.
	 * @type br.presenter.property.WritableProperty
	 */
	this.label = new WritableProperty('');

	/**
	 * The currently selected option, or potentially any string the user has entered if being displayed with a combo-box.
	 * @type br.presenter.property.WritableProperty
	 */
	this.value = null;

	/**
	 * A boolean property that is <code>true</code> if {@link #value} has any validation errors, and <code>false</code> otherwise.
	 * @type br.presenter.property.WritableProperty
	 */
	this.hasError = new WritableProperty(false);

	/**
	 * A textual description of the currently failing validation message when {@link #hasError} is <code>true</code>.
	 * @type br.presenter.property.WritableProperty
	 */
	this.failureMessage = new WritableProperty();

	/**
	 * A boolean property representing whether the selection field is enabled or not.
	 * @type br.presenter.property.WritableProperty
	 */
	this.enabled = new WritableProperty(true);

	/**
	 * A boolean property representing whether the selection field is visible or not.
	 * @type br.presenter.property.WritableProperty
	 */
	this.visible = new WritableProperty(true);

	/**
	 * The logical control-name the selection field is being bound to &mdash; this
	 * value will appear within the <code>name</code> attribute if being bound to a native HTML control.
	 * @type br.presenter.property.WritableProperty
	 */
	this.controlName = new WritableProperty('');

	/**
	 * The current list of options the user can select from.
	 * @type br.presenter.node.OptionsNodeList
	 */
	this.options = (vOptions instanceof OptionsNodeList) ? vOptions : new OptionsNodeList(vOptions);
	this.options.addChangeListener(this, '_automaticallyUpdateValueOnOptionsChange');

	if ((vValue instanceof Property) && !(vValue instanceof EditableProperty)) {
		throw new Errors.InvalidParametersError("SelectionField constructor: can't pass non-editable property as parameter");
	}

	/** @private */
	this.m_vDefaultValue = null;

	if (vValue instanceof EditableProperty) {
		this.m_vDefaultValue = vValue.getValue();
		this.value = vValue;
	} else {
		if (vValue === undefined) {
			var oFirstOption = this.options.getFirstOption(); // could be null
			this.m_vDefaultValue = oFirstOption ? oFirstOption.value.getValue() : null;
		} else {
			this.m_vDefaultValue = vValue;
		}

		this.value = new EditableProperty(this.m_vDefaultValue);
	}

	/** @private */
	this.m_oValidSelectionValidator = new ValidSelectionValidator(this.options);
	this.value.addValidator(this.m_oValidSelectionValidator);

	/** @private */
	this.m_oValueListener = new FieldValuePropertyListener(this);

	// validate the initial values
	this.value.forceValidation();

	/**
	 * The current text value of the selected option's label.
	 * @type {br.presenter.property.Property}
	 */
	this.selectedOptionLabel = new Property();
	this.value.addChangeListener(this, '_updateSelectedOptionLabel', true);
}

Core.extend(SelectionField, PresentationNode);

/**
 * Whether the selection field displays a validation error if the selected {@link #value} is not a member of
 * the {@link #options} array.
 * 
 * <p>Invalid selections cause validation errors by default, but this may not always be the desired behaviour,
 * for example if the <code>SelectionField</code> is being displayed using a combo-box, where the {@link #options}
 * are acting merely as suggestions, rather than as the absolute set of options.</p>
 * 
 * @param {boolean} bAllowInvalidSelections Invalid selections are allowed when set to <code>true</code>.
 */
SelectionField.prototype.allowInvalidSelections = function(bAllowInvalidSelections) {
	this.m_oValidSelectionValidator.allowInvalidSelections(bAllowInvalidSelections);
};

/**
 * Whether the selection field automatically picks a new {@link #value} when the underlying {@link #options}
 * change.
 * 
 * <p>If the underlying {@link #options} change, so that the new list of {@link #options} no longer includes
 * the currently selected {@link #value}, a validation error will be displayed by default. In some circumstances,
 * it may make sense to have the selection field automatically pick a new value automatically.</p>
 * 
 * @param {boolean} bAutomaticallyUpdate
 * @see #allowInvalidSelections
 */
SelectionField.prototype.automaticallyUpdateValueWhenOptionsChange = function(bAutomaticallyUpdate) {
	this.m_bAutomaticallyUpdateValueWhenOptionsChange = bAutomaticallyUpdate;
};

/**
 * @private
 */
SelectionField.prototype._automaticallyUpdateValueOnOptionsChange = function() {
	if (!this.m_bAutomaticallyUpdateValueWhenOptionsChange) {
		this.value.forceValidation();
	} else {
		var pOptions = this.options.getOptions();
		if (pOptions.length > 0) {
			var mOptions = MapUtility.addArrayToMap({}, pOptions);

			// if the currently selected value is no longer one of the available options, then revert to default or the first available option
			if (!mOptions[this.value.getValue()]) {
				var vNewValue = (mOptions[this.m_vDefaultValue]) ? this.m_vDefaultValue : pOptions[0].value.getValue();
				var oValueProperty = this.value;

				oValueProperty.setUserEnteredValue(vNewValue);
			}
		}
	}
};

/** @private */
SelectionField.prototype._updateSelectedOptionLabel = function() {
	var selectedOption = this.options.getOptionByValue(this.value.getValue());

	if (selectedOption) {
		this.selectedOptionLabel._$setInternalValue(selectedOption.label.getValue());
	} else {
		this.selectedOptionLabel._$setInternalValue('');
	}
};

module.exports = SelectionField;

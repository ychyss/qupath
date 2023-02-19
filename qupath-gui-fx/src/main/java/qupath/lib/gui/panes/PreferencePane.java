/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * Copyright (C) 2018 - 2022 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.lib.gui.panes;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.control.PropertySheet.Mode;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import qupath.lib.LocaleMessage;
import qupath.lib.common.GeneralTools;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.logging.LogManager;
import qupath.lib.gui.logging.LogManager.LogLevel;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.prefs.PathPrefs.AutoUpdateType;
import qupath.lib.gui.prefs.PathPrefs.DetectionTreeDisplayModes;
import qupath.lib.gui.prefs.PathPrefs.FontSize;
import qupath.lib.gui.prefs.PathPrefs.LabelLocation;
import qupath.lib.gui.prefs.PathPrefs.ImageTypeSetting;
import qupath.lib.gui.prefs.PathPrefs.Delimiter;
import qupath.lib.gui.tools.ColorToolsFX;
import qupath.lib.gui.tools.CommandFinderTools;
import qupath.lib.gui.tools.CommandFinderTools.CommandBarDisplay;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.gui.prefs.QuPathStyleManager;

/**
 * Basic preference panel, giving a means to modify some of the properties within PathPrefs.
 * 
 * @author Pete Bankhead
 *
 */
public class PreferencePane {

	private static final Logger logger = LoggerFactory.getLogger(PreferencePane.class);

	private PropertySheet propSheet = new PropertySheet();

	@SuppressWarnings("javadoc")
	public PreferencePane() {
		setupPanel();
	}

	
	private void addCategoryAppearance() {
		/*
		 * Appearance
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Appearance");

		addChoicePropertyPreference(QuPathStyleManager.selectedStyleProperty(),
				QuPathStyleManager.availableStylesProperty(),
				QuPathStyleManager.StyleOption.class,
				LocaleMessage.getInstance().get("PreferencePane.Appearance.Theme"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Appearance.Theme.Description"));
		
		addChoicePropertyPreference(QuPathStyleManager.fontProperty(),
				QuPathStyleManager.availableFontsProperty(),
				QuPathStyleManager.Fonts.class,
				LocaleMessage.getInstance().get("PreferencePane.Appearance.Font"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Appearance.Font.Description"));
	}
	
	private void addCategoryGeneral() {
		/*
		 * General
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.General");
		
		boolean canSetMemory = PathPrefs.hasJavaPreferences();
		long maxMemoryMB = Runtime.getRuntime().maxMemory() / 1024 / 1024;
		if (canSetMemory) {
			DoubleProperty propMemoryGB = new SimpleDoubleProperty(maxMemoryMB / 1024.0);
			
			addPropertyPreference(propMemoryGB, Double.class, 
					LocaleMessage.getInstance().get("PreferencePane.General.MaxMemory"),
					category,
					LocaleMessage.getInstance().get("PreferencePane.General.MaxMemory.Description"));
			
			propMemoryGB.addListener((v, o, n) -> {
				int requestedMemoryMB = (int)Math.round(propMemoryGB.get() * 1024.0);
				if (requestedMemoryMB > 1024) {
					boolean success = false;
					try {
						PathPrefs.maxMemoryMBProperty().set(requestedMemoryMB);		
						success = requestedMemoryMB == PathPrefs.maxMemoryMBProperty().get();
					} catch (Exception e) {
						logger.error(e.getLocalizedMessage(), e);
					}
					if (success) {
						Dialogs.showInfoNotification("Set max memory",
								"Setting max memory to " + requestedMemoryMB + " MB - you'll need to restart QuPath for this to take effect"
								);
					} else {
						Dialogs.showErrorMessage("Set max memory",
								"Unable to set max memory - sorry!\n"
								+ "Check the FAQs on ReadTheDocs for details how to set the "
								+ "memory limit by editing QuPath's config file."
								);						
					}
				}
			});	
		}
		
		
		addPropertyPreference(PathPrefs.showStartupMessageProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowWelcomeMessage"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowWelcomeMessage.Description"));
		
		addPropertyPreference(PathPrefs.autoUpdateCheckProperty(), AutoUpdateType.class,
				LocaleMessage.getInstance().get("PreferencePane.General.CheckForUpdates"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.CheckForUpdates.Description"));

		addPropertyPreference(PathPrefs.runStartupScriptProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.RunStartupScript"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.RunStartupScript.Description"));

		addPropertyPreference(PathPrefs.useSystemMenubarProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.UseSystemMenubar"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.UseSystemMenubar.Description"));
				
		addPropertyPreference(PathPrefs.tileCachePercentageProperty(),
				Double.class,
				LocaleMessage.getInstance().get("PreferencePane.General.PercentageMemoryForTileCaching"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.PercentageMemoryForTileCaching.Description"));
		
		addPropertyPreference(PathPrefs.showImageNameInTitleProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowImageNameInTitle"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowImageNameInTitle.Description"));
		
		addPropertyPreference(PathPrefs.maskImageNamesProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowMaskImageNamesInProjects"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowMaskImageNamesInProjects.Description"));
		
		
		addPropertyPreference(PathPrefs.doCreateLogFilesProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.CreateLogFiles"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.CreateLogFiles.Description"));
		
		addPropertyPreference(LogManager.rootLogLevelProperty(), LogManager.LogLevel.class,
				LocaleMessage.getInstance().get("PreferencePane.General.Loglevel"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.Loglevel.Description"));

		addPropertyPreference(PathPrefs.numCommandThreadsProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.General.NumberOfThreads"),
				category,
				"Set limit on number of processors to use for parallelization."
						+ "\nThis should be > 0 and <= the available processors on the computer."
						+ "\nIf outside this range, it will default to the available processors (here, " + Runtime.getRuntime().availableProcessors() + ")"
						+ "\nIt's usually fine to use the default, but it may help to decrease it if you encounter out-of-memory errors.");

		addPropertyPreference(PathPrefs.imageTypeSettingProperty(), ImageTypeSetting.class,
				LocaleMessage.getInstance().get("PreferencePane.General.SetImageType"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.SetImageType.Description"));

		addPropertyPreference(CommandFinderTools.commandBarDisplayProperty(), CommandBarDisplay.class,
				LocaleMessage.getInstance().get("PreferencePane.General.CommandBarDisplayMode"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.CommandBarDisplayMode.Description"));
		
		addPropertyPreference(PathPrefs.showExperimentalOptionsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowExperimentalMenuItem"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowExperimentalMenuItem.Description"));

		addPropertyPreference(PathPrefs.showTMAOptionsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowTMAMenu"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowTMAMenu.Description"));
		
		addPropertyPreference(PathPrefs.showLegacyOptionsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowLegacyMenuItems"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.ShowLegacyMenuItems.Description"));

		addPropertyPreference(PathPrefs.detectionTreeDisplayModeProperty(), DetectionTreeDisplayModes.class,
				LocaleMessage.getInstance().get("PreferencePane.General.HierarchyDetectionDisplay"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.General.HierarchyDetectionDisplay.Description"));
	}
	
	
	private void addCategoryLocale() {
		/*
		 * Locale
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Locale");
		var localeList = FXCollections.observableArrayList(
				Arrays.stream(Locale.getAvailableLocales())
				.filter(l -> !l.getLanguage().isBlank())
				.sorted(Comparator.comparing(l -> l.getDisplayName(Locale.US)))
				.collect(Collectors.toList())
				);
		
		// Would like to use a searchable combo box,
		// but https://github.com/controlsfx/controlsfx/issues/1413 is problematic
		var localeSearchable = false;
//		logger.info("add locale(default):"+ PathPrefs.defaultLocaleProperty().get().toString());
		addChoicePropertyPreference(PathPrefs.defaultLocaleProperty(), localeList, Locale.class,
				LocaleMessage.getInstance().get("PreferencePane.Locale.MainDefaultLocale"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Locale.MainDefaultLocale.Description"),
				localeSearchable);

		addChoicePropertyPreference(PathPrefs.defaultLocaleDisplayProperty(), localeList, Locale.class,
				LocaleMessage.getInstance().get("PreferencePane.Locale.DisplayLocale"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Locale.DisplayLocale.Description"),
				localeSearchable);
		
		addChoicePropertyPreference(PathPrefs.defaultLocaleFormatProperty(), localeList, Locale.class,
				LocaleMessage.getInstance().get("PreferencePane.Locale.FormatLocale"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Locale.FormatLocale.Description"),
				localeSearchable);
	}
	
	private void addCategoryInputOutput() {
		/*
		 * Input/output
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.IO");
		
		addPropertyPreference(PathPrefs.minPyramidDimensionProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.IO.MinimizeImageDimensionForPyramidalizing"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.IO.MinimizeImageDimensionForPyramidalizing.Description"));

		
		addPropertyPreference(PathPrefs.tmaExportDownsampleProperty(), Double.class,
			LocaleMessage.getInstance().get("PreferencePane.IO.TMAExportDownsampleFactor"),
			category,
			LocaleMessage.getInstance().get("PreferencePane.IO.TMAExportDownsampleFactor.Description"));
	}

	private void addCategoryViewer() {
		/*
		 * Viewer
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Viewer");
		
		addColorPropertyPreference(PathPrefs.viewerBackgroundColorProperty(),
				LocaleMessage.getInstance().get("PreferencePane.Viewer.BackgroundColor"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.BackgroundColor.Description"));
		
		addPropertyPreference(PathPrefs.alwaysPaintSelectedObjectsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.AlwaysPaintSelectedObjects"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.AlwaysPaintSelectedObjects.Description"));
		
		addPropertyPreference(PathPrefs.keepDisplaySettingsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.KeepDisplaySettingsWherePossible"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.KeepDisplaySettingsWherePossible.Description"));
		
		addPropertyPreference(PathPrefs.viewerInterpolateBilinearProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseBilinearInterpolation"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseBilinearInterpolation.Description"));
		
		addPropertyPreference(PathPrefs.autoBrightnessContrastSaturationPercentProperty(), Double.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.AutoContrastSaturation"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.AutoContrastSaturation.Description"));
		
//		addPropertyPreference(PathPrefs.viewerGammaProperty(), Double.class,
//				"Gamma value (display only)", category, 
//				"Set the gamma value applied to the image in the viewer for display - recommended to leave at default value of 1");
		
		addPropertyPreference(PathPrefs.invertZSliderProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.InvertZSlider"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.InvertZSlider.Description"));
		
		addPropertyPreference(PathPrefs.scrollSpeedProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScrollSpeed"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScrollSpeed.Description"));
		
		addPropertyPreference(PathPrefs.navigationSpeedProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.NavigationSpeed"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.NavigationSpeed.Description"));
		
		addPropertyPreference(PathPrefs.navigationAccelerationProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.NavigationAccelerationEffects"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.NavigationAccelerationEffects.Description"));
		
		addPropertyPreference(PathPrefs.skipMissingCoresProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.SkipMissingTMACores"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.SkipMissingTMACores.Description"));
		
		addPropertyPreference(PathPrefs.useScrollGesturesProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseScrollTouchGestures"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseScrollTouchGestures.Description"));

		addPropertyPreference(PathPrefs.invertScrollingProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.InvertScrolling"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.InvertScrolling.Description"));

		addPropertyPreference(PathPrefs.useZoomGesturesProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseZoomTouchGestures"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseZoomTouchGestures.Description"));

		addPropertyPreference(PathPrefs.useRotateGesturesProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseRotateTouchGestures"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseRotateTouchGestures.Description"));
		
		addPropertyPreference(PathPrefs.enableFreehandToolsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.EnableFreehandModeANDPolylineTools"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.EnableFreehandModeANDPolylineTools.Description"));
		
		addPropertyPreference(PathPrefs.doubleClickToZoomProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseDoubleClickToZoom"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseDoubleClickToZoom.Description"));

		addPropertyPreference(PathPrefs.scalebarFontSizeProperty(), FontSize.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarFontSize"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarFontSize.Description"));
		
		addPropertyPreference(PathPrefs.scalebarFontWeightProperty(), FontWeight.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarFontWeight"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarFontWeight.Description"));

		addPropertyPreference(PathPrefs.scalebarLineWidthProperty(), Double.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarThickness"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarThickness.Description"));

		addPropertyPreference(PathPrefs.scalebarLocationProperty(), LabelLocation.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarLocation"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.ScalebarLocation.Description"));
		addPropertyPreference(PathPrefs.locationFontSizeProperty(), FontSize.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.LocationTextFontSize"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.LocationTextFontSize.Description"));

		addPropertyPreference(PathPrefs.useCalibratedLocationStringProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseCalibratedLocationText"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.UseCalibratedLocationText.Description1")
						+ GeneralTools.micrometerSymbol()
						+ LocaleMessage.getInstance().get("PreferencePane.Viewer.UseCalibratedLocationText.Description2"));
		addPropertyPreference(PathPrefs.locInfoLocationProperty(), LabelLocation.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.LocInfoLocation"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.LocInfoLocation.Description"));
		addPropertyPreference(PathPrefs.overviewLocationProperty(), LabelLocation.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.OverviewLocation"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.OverviewLocation.Description"));

		
		addPropertyPreference(PathPrefs.gridSpacingXProperty(), Double.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingX"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingX.Description"));

		addPropertyPreference(PathPrefs.gridSpacingYProperty(), Double.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingY"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingY.Description"));

		addPropertyPreference(PathPrefs.gridScaleMicronsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingIn")
						+ " "
						+ GeneralTools.micrometerSymbol(),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingIn.Description1")
						+ " "
						+ GeneralTools.micrometerSymbol()
						+ " "
						+ LocaleMessage.getInstance().get("PreferencePane.Viewer.GridSpacingIn.Description2"));
	}

	private void addCategoryExtensions() {
		/*
		 * Extensions
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Extensions");
		addDirectoryPropertyPreference(PathPrefs.userPathProperty(),
				LocaleMessage.getInstance().get("PreferencePane.Extensions.QuPathUserDirectory"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Extensions.QuPathUserDirectory.Description"));

	}
	
	private void addCategoryMeasurements() {
		/*
		 * Drawing tools
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Measurements");
		addPropertyPreference(PathPrefs.showMeasurementTableThumbnailsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.IncludeThumbnailColumnInMeasurementTables"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.IncludeThumbnailColumnInMeasurementTables.Description"));
		
		addPropertyPreference(PathPrefs.showMeasurementTableObjectIDsProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.IncludeObjectIDColumnInMeasurementTables"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.IncludeObjectIDColumnInMeasurementTables.Description"));

		addPropertyPreference(PathPrefs.tableDelimiterProperty(), Delimiter.class,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.tableDelimiter"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Measurements.tableDelimiter.Description"));


	}
	
	private void addCategoryAutomation() {
		/*
		 * Automation
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.Automation");
		addDirectoryPropertyPreference(PathPrefs.scriptsPathProperty(),
				LocaleMessage.getInstance().get("PreferencePane.Automation.ScriptDirectory"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.Automation.ScriptDirectory.Description"));

	}

	private void addCategoryDrawingTools() {
		/*
		 * Drawing tools
		 */
		String category = LocaleMessage.getInstance().get("PreferencePane.DrawingTools");
		addPropertyPreference(PathPrefs.returnToMoveModeProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ReturnToMoveToolAutomatically"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ReturnToMoveToolAutomatically.Description"));
		
		addPropertyPreference(PathPrefs.usePixelSnappingProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UsePixelSnapping"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UsePixelSnapping.Description"));
		
		addPropertyPreference(PathPrefs.clipROIsForHierarchyProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ClipROIsToHierarchy"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ClipROIsToHierarchy.Description"));

		addPropertyPreference(PathPrefs.brushDiameterProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.BrushDiameter"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.BrushDiameter.Description"));
		
		addPropertyPreference(PathPrefs.useTileBrushProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UseTileBrush"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UseTileBrush.Description"));

		addPropertyPreference(PathPrefs.brushScaleByMagProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ScaleBrushByMagnification"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.ScaleBrushByMagnification.Description"));
		
		addPropertyPreference(PathPrefs.multipointToolProperty(), Boolean.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UseMultipointTool"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.UseMultipointTool.Description"));

		addPropertyPreference(PathPrefs.pointRadiusProperty(), Integer.class,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.PointRadius"),
				category,
				LocaleMessage.getInstance().get("PreferencePane.DrawingTools.PointRadius.Description"));
	}
	
	private void addCategoryObjects() {
		/*
		 * Object colors
		 */
		String category = "Objects";
		
		addPropertyPreference(PathPrefs.maxObjectsToClipboardProperty(), Integer.class,
				"Maximum number of clipboard objects",
				category,
				"The maximum number of objects that can be copied to the system clipboard.\n"
				+ "Attempting to copy too many may fail, or cause QuPath to hang.\n"
				+ "If you need more objects, it is better to export as GeoJSON and then import later.");

		addPropertyPreference(PathPrefs.annotationStrokeThicknessProperty(), Float.class,
				"Annotation line thickness",
				category,
				"Thickness (in display pixels) for annotation/TMA core object outlines (default = 2)");

		addPropertyPreference(PathPrefs.detectionStrokeThicknessProperty(), Float.class,
				"Detection line thickness",
				category,
				"Thickness (in image pixels) for detection object outlines (default = 2)");

		addPropertyPreference(PathPrefs.useSelectedColorProperty(), Boolean.class,
				"Use selected color",
				category,
				"Highlight selected objects by recoloring them; otherwise, a slightly thicker line thickness will be used");

		addColorPropertyPreference(PathPrefs.colorSelectedObjectProperty(),
				"Selected object color",
				category,
				"Set the color used to highly the selected object");

		addColorPropertyPreference(PathPrefs.colorDefaultObjectsProperty(),
				"Default object color",
				category,
				"Set the default color for objects");

		addColorPropertyPreference(PathPrefs.colorTMAProperty(),
				"TMA core color",
				category,
				"Set the default color for TMA core objects");

		addColorPropertyPreference(PathPrefs.colorTMAMissingProperty(),
				"TMA missing core color",
				category,
				"Set the default color for missing TMA core objects");
	}

	private void setupPanel() {
		//		propSheet.setMode(Mode.CATEGORY);
		propSheet.setMode(Mode.CATEGORY);
		propSheet.setPropertyEditorFactory(new PropertyEditorFactory());

		addCategoryAppearance();
		addCategoryGeneral();
		addCategoryLocale();
		addCategoryInputOutput();
		addCategoryViewer();
		addCategoryExtensions();
		addCategoryMeasurements();
		addCategoryAutomation();
		addCategoryDrawingTools();
		addCategoryObjects();
	}

	/**
	 * Get the property sheet for this {@link PreferencePane}.
	 * This is a {@link Node} that may be added to a scene for display.
	 * @return
	 */
	public PropertySheet getPropertySheet() {
		return propSheet;
	}


	
	/**
	 * Add a new preference based on a specified Property.
	 * 
	 * @param prop
	 * @param cls
	 * @param name
	 * @param category
	 * @param description
	 */
	public <T> void addPropertyPreference(final Property<T> prop, final Class<? extends T> cls, final String name, final String category, final String description) {
		PropertySheet.Item item = new DefaultPropertyItem<>(prop, cls)
				.name(name)
				.category(category)
				.description(description);
		propSheet.getItems().add(item);
	}
	
	
	/**
	 * Add a new color preference based on a specified IntegerProperty (storing a packed RGBA value).
	 * 
	 * @param prop
	 * @param name
	 * @param category
	 * @param description
	 */
	public void addColorPropertyPreference(final IntegerProperty prop, final String name, final String category, final String description) {
		PropertySheet.Item item = new ColorPropertyItem(prop)
				.name(name)
				.category(category)
				.description(description);
		propSheet.getItems().add(item);
	}
	
	
	/**
	 * Add a new directory preference based on a specified StrongProperty.
	 * 
	 * @param prop
	 * @param name
	 * @param category
	 * @param description
	 */
	public void addDirectoryPropertyPreference(final Property<String> prop, final String name, final String category, final String description) {
		PropertySheet.Item item = new DirectoryPropertyItem(prop)
				.name(name)
				.category(category)
				.description(description);
		propSheet.getItems().add(item);
	}
	
	
	/**
	 * Add a new choice preference, to select from a list of possibilities.
	 * 
	 * @param prop
	 * @param choices
	 * @param cls
	 * @param name
	 * @param category
	 * @param description
	 */
	public <T> void addChoicePropertyPreference(final Property<T> prop, final ObservableList<T> choices, final Class<? extends T> cls, final String name, final String category, final String description) {
		addChoicePropertyPreference(prop, choices, cls, name, category, description, false);
	}
	

	/**
	 * Add a new choice preference, to select from an optionally searchable list of possibilities.
	 * 
	 * @param prop
	 * @param choices
	 * @param cls
	 * @param name
	 * @param category
	 * @param description
	 * @param makeSearchable make the choice item's editor searchable (useful for long lists)
	 */
	public <T> void addChoicePropertyPreference(final Property<T> prop, final ObservableList<T> choices, final Class<? extends T> cls, 
			final String name, final String category, final String description, boolean makeSearchable) {
		PropertySheet.Item item = new ChoicePropertyItem<>(prop, choices, cls, makeSearchable)
				.name(name)
				.category(category)
				.description(description);
		propSheet.getItems().add(item);
	}
	
	/**
	 * Create a default {@link Item} for a generic property.
	 * @param <T> type of the property
	 * @param property the property
	 * @param cls the property type
	 * @return a new {@link PropertyItem}
	 */
	public static <T> PropertyItem createPropertySheetItem(Property<T> property, Class<? extends T> cls) {
		return new DefaultPropertyItem<>(property, cls);
	}
	
	
	/**
	 * Base implementation of {@link Item}.
	 */
	public abstract static class PropertyItem implements PropertySheet.Item {

		private String name;
		private String category;
		private String description;

		/**
		 * Support fluent interface to define a category.
		 * @param category
		 * @return
		 */
		public PropertyItem category(final String category) {
			this.category = category;
			return this;
		}

		/**
		 * Support fluent interface to set the description.
		 * @param description
		 * @return
		 */
		public PropertyItem description(final String description) {
			this.description = description;
			return this;
		}

		/**
		 * Support fluent interface to set the name.
		 * @param name
		 * @return
		 */
		public PropertyItem name(final String name) {
			this.name = name;
			return this;
		}

		@Override
		public String getCategory() {
			return category;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

	}


	static class DefaultPropertyItem<T> extends PropertyItem {

		private Property<T> prop;
		private Class<? extends T> cls;

		DefaultPropertyItem(final Property<T> prop, final Class<? extends T> cls) {
			this.prop = prop;
			this.cls = cls;
		}

		@Override
		public Class<?> getType() {
			return cls;
		}

		@Override
		public Object getValue() {
			return prop.getValue();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setValue(Object value) {
			prop.setValue((T)value);
		}

		@Override
		public Optional<ObservableValue<? extends Object>> getObservableValue() {
			return Optional.of(prop);
		}

	}


	/**
	 * Create a property item that handles directories based on String paths.
	 */
	static class DirectoryPropertyItem extends PropertyItem {

		private Property<String> prop;
		private ObservableValue<File> fileValue;

		DirectoryPropertyItem(final Property<String> prop) {
			this.prop = prop;
			fileValue = Bindings.createObjectBinding(() -> prop.getValue() == null ? null : new File(prop.getValue()), prop);
		}

		@Override
		public Class<?> getType() {
			return File.class;
		}

		@Override
		public Object getValue() {
			return fileValue.getValue();
		}

		@Override
		public void setValue(Object value) {
			if (value instanceof String)
				prop.setValue((String)value);
			else if (value instanceof File)
				prop.setValue(((File)value).getAbsolutePath());
			else if (value == null)
				prop.setValue(null);
			else
				logger.error("Cannot set property {} with value {}", prop, value);
		}

		@Override
		public Optional<ObservableValue<? extends Object>> getObservableValue() {
			return Optional.of(fileValue);
		}

	}


	static class ColorPropertyItem extends PropertyItem {

		private IntegerProperty prop;
		private ObservableValue<Color> value;

		ColorPropertyItem(final IntegerProperty prop) {
			this.prop = prop;
			this.value = Bindings.createObjectBinding(() -> ColorToolsFX.getCachedColor(prop.getValue()), prop);
		}

		@Override
		public Class<?> getType() {
			return Color.class;
		}

		@Override
		public Object getValue() {
			return value.getValue();
		}

		@Override
		public void setValue(Object value) {
			if (value instanceof Color)
				value = ColorToolsFX.getARGB((Color)value);
			if (value instanceof Integer)
				prop.setValue((Integer)value);
		}

		@Override
		public Optional<ObservableValue<? extends Object>> getObservableValue() {
			return Optional.of(value);
		}

	}
	
	
	static class ChoicePropertyItem<T> extends DefaultPropertyItem<T> {

		private final ObservableList<T> choices;
		private final boolean makeSearchable;
		
		ChoicePropertyItem(final Property<T> prop, final ObservableList<T> choices, final Class<? extends T> cls) {
			this(prop, choices, cls, false);
		}

		ChoicePropertyItem(final Property<T> prop, final ObservableList<T> choices, final Class<? extends T> cls, boolean makeSearchable) {
			super(prop, cls);
			this.choices = choices;
			this.makeSearchable = makeSearchable;
		}
		
		public ObservableList<T> getChoices() {
			return choices;
		}
		
		public boolean makeSearchable() {
			return makeSearchable;
		}

	}



	/**
	 * Editor for selecting directory paths.
	 * 
	 * Appears as a text field that can be double-clicked to launch a directory chooser.
	 */
	static class DirectoryEditor extends AbstractPropertyEditor<File, TextField> {

		private ObservableValue<File> value;

		public DirectoryEditor(Item property, TextField control) {
			super(property, control, true);
			control.setOnMouseClicked(e -> {
				if (e.getClickCount() > 1) {
					e.consume();
					File dirNew = Dialogs.getChooser(control.getScene().getWindow()).promptForDirectory(getValue());
					if (dirNew != null)
						setValue(dirNew);
				}
			});
			if (property.getDescription() != null) {
				var description = property.getDescription();
				var tooltip = new Tooltip(description);
				tooltip.setShowDuration(Duration.millis(10_000));
				control.setTooltip(tooltip);
			}
			
			// Bind to the text property
			if (property instanceof DirectoryPropertyItem) {
				control.textProperty().bindBidirectional(((DirectoryPropertyItem)property).prop);
			}
			value = Bindings.createObjectBinding(() -> {
				String text = control.getText();
				if (text == null || text.trim().isEmpty() || !new File(text).isDirectory())
					return null;
				else
					return new File(text);
				}, control.textProperty());
		}

		@Override
		public void setValue(File value) {
			getEditor().setText(value == null ? null : value.getAbsolutePath());
		}

		@Override
		protected ObservableValue<File> getObservableValue() {
			return value;
		}

	}
	
	
	/**
	 * Editor for choosing from a longer list of items, aided by a searchable combo box.
	 * @param <T> 
	 */
	static class SearchableChoiceEditor<T> extends AbstractPropertyEditor<T, SearchableComboBox<T>> {

		public SearchableChoiceEditor(Item property, Collection<? extends T> choices) {
			this(property, FXCollections.observableArrayList(choices));
		}

		public SearchableChoiceEditor(Item property, ObservableList<T> choices) {
			super(property, new SearchableComboBox<T>());
			getEditor().setItems(choices);
		}

		@Override
		public void setValue(T value) {
			// Only set the value if it's available as a choice
			if (getEditor().getItems().contains(value))
				getEditor().getSelectionModel().select(value);
		}

		@Override
		protected ObservableValue<T> getObservableValue() {
			return getEditor().getSelectionModel().selectedItemProperty();
		}
		
	}
	
	/**
	 * Editor for choosing from a combo box, which will use an observable list directly if it can 
	 * (which differs from ControlsFX's default behavior).
	 *
	 * @param <T>
	 */
	static class ChoiceEditor<T> extends AbstractPropertyEditor<T, ComboBox<T>> {

		public ChoiceEditor(Item property, Collection<? extends T> choices) {
			this(property, FXCollections.observableArrayList(choices));
		}

		public ChoiceEditor(Item property, ObservableList<T> choices) {
			super(property, new ComboBox<T>());
			getEditor().setItems(choices);
		}

		@Override
		public void setValue(T value) {
			// Only set the value if it's available as a choice
			if (getEditor().getItems().contains(value))
				getEditor().getSelectionModel().select(value);
		}

		@Override
		protected ObservableValue<T> getObservableValue() {
			return getEditor().getSelectionModel().selectedItemProperty();
		}
		
	}
	
	
	// We want to reformat the display of these to avoid using all uppercase
	private static Map<Class<?>, Function<?, String>> reformatTypes = Map.of(
			FontWeight.class, PreferencePane::simpleFormatter,
			LogLevel.class, PreferencePane::simpleFormatter,
			Locale.class, PreferencePane::localeFormatter
			);
	
	private static String simpleFormatter(Object obj) {
		var s = Objects.toString(obj);
		s = s.replaceAll("_", " ");
		if (Objects.equals(s, s.toUpperCase()))
			return s.substring(0, 1) + s.substring(1).toLowerCase();
		return s;
	}
	
	private static String localeFormatter(Object locale) {
		return locale == null ? null : ((Locale)locale).getDisplayName(Locale.US);
	}

	/**
	 * Extends {@link DefaultPropertyEditorFactory} to handle setting directories and creating choice editors.
	 */
	public static class PropertyEditorFactory extends DefaultPropertyEditorFactory {

		@SuppressWarnings("unchecked")
		@Override
		public PropertyEditor<?> call(Item item) {
			if (item.getType() == File.class) {
				return new DirectoryEditor(item, new TextField());
			}
			PropertyEditor<?> editor;
			if (item instanceof ChoicePropertyItem) {
				var choiceItem = ((ChoicePropertyItem<?>)item);
				if (choiceItem.makeSearchable()) {
					editor = new SearchableChoiceEditor<>(choiceItem, choiceItem.getChoices());
				} else
					// Use this rather than Editors because it wraps an existing ObservableList where available
					editor = new ChoiceEditor<>(choiceItem, choiceItem.getChoices());
//					editor = Editors.createChoiceEditor(item, choiceItem.getChoices());
			} else
				editor = super.call(item);
			
			if (reformatTypes.containsKey(item.getType()) && editor.getEditor() instanceof ComboBox) {
				@SuppressWarnings("rawtypes")
				var combo = (ComboBox)editor.getEditor();
				var formatter = reformatTypes.get(item.getType());
				combo.setCellFactory(obj -> GuiTools.createCustomListCell(formatter));
				combo.setButtonCell(GuiTools.createCustomListCell(formatter));
			}
			
			// Make it easier to reset default locale
			if (Locale.class.equals(item.getType())) {
				editor.getEditor().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
					if (e.getClickCount() == 2) {
						if (Dialogs.showConfirmDialog("Reset locale", "Reset locale to 'English (United States)'?"))
							item.setValue(Locale.US);
					}
				});
			}
			
			return editor;
		}
	}
	
}

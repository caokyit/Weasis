/*******************************************************************************
 * Copyright (c) 2011 Weasis Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.core.ui.graphic;

import org.weasis.core.ui.Messages;

public interface ImageStatistics {
    Measurement IMAGE_MIN = new Measurement(Messages.getString("measure.min"), 1, false, true, false); //$NON-NLS-1$
    Measurement IMAGE_MAX = new Measurement(Messages.getString("measure.max"), 2, false, true, false); //$NON-NLS-1$
    Measurement IMAGE_MEAN = new Measurement(Messages.getString("measure.mean"), 3, false, true, true); //$NON-NLS-1$
    Measurement IMAGE_STD = new Measurement(Messages.getString("measure.stdev"), 4, false, true, false); //$NON-NLS-1$
    Measurement IMAGE_SKEW = new Measurement(Messages.getString("measure.skew"), 5, false, true, false); //$NON-NLS-1$
    Measurement IMAGE_KURTOSIS = new Measurement(Messages.getString("measure.kurtosis"), 6, false, true, false); //$NON-NLS-1$

    Measurement[] ALL_MEASUREMENTS = { IMAGE_MIN, IMAGE_MAX, IMAGE_MEAN, IMAGE_STD, IMAGE_SKEW, IMAGE_KURTOSIS };
}

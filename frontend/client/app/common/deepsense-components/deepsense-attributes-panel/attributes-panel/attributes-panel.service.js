/**
 * Copyright 2017, deepsense.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

function AttributesPanelService() {
  this.disabledMode = false;

  this.setDisabledMode = function () {
    this.disabledMode = true;
  };

  this.setEnabledMode = function () {
    this.disabledMode = false;
  };

  this.getDisabledMode = function () {
    return this.disabledMode;
  };

  this.disableElements = function (container) {
    if (this.getDisabledMode()) {
      jQuery(':input:not(.o-error-btn), textarea', container)
        .attr('disabled', 'disabled');
    }
  };

  this.enableElements = function (container) {
    jQuery(':input:not(.o-error-btn), textarea', container)
      .removeAttr('disabled');
  };
}

angular.module('deepsense.attributes-panel')
  .service('AttributesPanelService', AttributesPanelService);

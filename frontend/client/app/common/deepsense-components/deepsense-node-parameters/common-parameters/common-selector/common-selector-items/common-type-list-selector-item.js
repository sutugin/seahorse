/**
 * Copyright 2015, deepsense.ai
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

let GenericSelectorItem = require('./common-generic-selector-item.js');

function TypeListSelectorItem(options) {
  this.type = TypeListSelectorItem.getType();

  this.types = {
    'numeric': false,
    'boolean': false,
    'string': false,
    'timestamp': false
  };

  let types = options.item.values;
  for (let i = 0; i < types.length; ++i) {
    this.types[types[i]] = true;
  }
}

TypeListSelectorItem.prototype = new GenericSelectorItem();
TypeListSelectorItem.prototype.constructor = GenericSelectorItem;

TypeListSelectorItem.prototype.serialize = function () {
  return {
    'type': 'typeList',
    'values': Object.keys(this.types).filter((type) => this.types[type])
  };
};

TypeListSelectorItem.getType = () => {
  return {
    'id': 'typeList',
    'verbose': 'Types list'
  };
};

TypeListSelectorItem.prototype.containsField = function(field) {
  return this.types[field.deeplangType];
};

module.exports = TypeListSelectorItem;

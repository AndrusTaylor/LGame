/**
 * Copyright 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.4.2
 */
package loon.component.table;

import loon.LSystem;
import loon.utils.TArray;

public class SimpleTableModel implements ITableModel {

	private TArray<ListItem> _list;

	public SimpleTableModel(TArray<ListItem> list) {
		_list = list;
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (_list.size == 0) {
			return "...";
		}
		return _list.get(columnIndex).name;
	}

	@Override
	public int getColumnCount() {
		return _list.size;
	}

	@Override
	public Object getValue(int row, int column) {
		if (column > _list.size || row > _list.get(column).list.size) {
			return "...";
		}
		ListItem items = null;
		if (_list.size > column) {
			items = _list.get(column);
		}
		if (items != null && items.list.size > row) {
			return items.list.get(row);
		}
		return LSystem.EMPTY;
	}

	@Override
	public int getRowCount() {
		if (_list.size == 0) {
			return 0;
		}
		return _list.get(0).list.size;
	}

	public void clear() {
		_list.clear();
	}

	public Object getValue(int row) {
		if (_list.size == 0) {
			return "...";
		}
		return _list.get(0).list.get(row);
	}
}

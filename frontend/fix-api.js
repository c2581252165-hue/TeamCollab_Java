const fs = require('fs');
const path = require('path');

function walk(dir) {
  let results = [];
  const list = fs.readdirSync(dir);
  list.forEach(function(file) {
    file = dir + '/' + file;
    const stat = fs.statSync(file);
    if (stat && stat.isDirectory()) { 
      results = results.concat(walk(file));
    } else { 
      if (file.endsWith('.vue')) results.push(file);
    }
  });
  return results;
}

const files = walk('D:/ruanjian/frontend/src/views/multicli');

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  
  if (content.includes("import axios from 'axios'")) {
    const parts = file.replace(/\\/g, '/').split('src/views/multicli/')[1].split('/');
    let relative = '../../utils/api';
    for (let i = 1; i < parts.length; i++) {
      relative = '../' + relative;
    }
    
    content = content.replace(/import axios from 'axios'/g, `import api from '${relative}'`);
    content = content.replace(/axios\.get/g, 'api.get');
    content = content.replace(/axios\.post/g, 'api.post');
    content = content.replace(/axios\.put/g, 'api.put');
    content = content.replace(/axios\.delete/g, 'api.delete');
    
    // Fix double data
    content = content.replace(/\.data\.data/g, '.data');
    
    fs.writeFileSync(file, content, 'utf8');
    console.log('Fixed', file);
  }
});

//GPT em arel 
async function loadProducts() {
  const res = await fetch('/products');
  const data = await res.json();
  const list = document.getElementById('product-list');
  list.innerHTML = '';

  data.forEach(p => {
    const div = document.createElement('div');
    div.classList.add('product-item');
    div.innerHTML = `<strong>${p.product}</strong> (age: ${p.age})`;
    list.appendChild(div);
  });
}


document.getElementById('product-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const product = document.getElementById('product').value;
  const age = document.getElementById('age').value;

  const res = await fetch('/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ product, age })
  });

  const msg = await res.text();
  document.getElementById('message').textContent = msg;

  loadProducts();
  e.target.reset();
});

loadProducts();
